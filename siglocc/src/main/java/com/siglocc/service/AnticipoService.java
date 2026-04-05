package com.siglocc.service;

import com.siglocc.dto.AnticipoRequest;
import com.siglocc.dto.AnticipoResponse;
import com.siglocc.entity.*;
import com.siglocc.repository.SolicitudAnticipoRepository;
import com.siglocc.repository.TemporadaRepository;
import com.siglocc.repository.UsuarioRepository;
import com.siglocc.repository.VistaControlSaldosRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Servicio que contiene la lógica de negocio del módulo de anticipos.
 *
 * <p>Implementa dos operaciones principales:</p>
 * <ul>
 *   <li>{@link #crearSolicitud} – valida saldo, persiste la solicitud y notifica por correo</li>
 *   <li>{@link #aprobarSolicitud} – cambia el estado a APROBADO y notifica al solicitante</li>
 * </ul>
 *
 * <p><strong>Patrón de envío de correos:</strong> Los correos se registran como
 * callbacks {@code afterCommit} usando {@link TransactionSynchronizationManager}.
 * Esto garantiza que el correo se dispara <em>solo si la transacción de BD se
 * completó exitosamente</em>. El envío en sí es asíncrono (ver
 * {@link EmailService}), por lo que el usuario recibe la respuesta HTTP
 * de inmediato sin esperar al servidor SMTP.</p>
 */
@Service
public class AnticipoService {

    private final SolicitudAnticipoRepository solicitudRepo;
    private final VistaControlSaldosRepository saldosRepo;
    private final UsuarioRepository usuarioRepository;
    private final TemporadaRepository temporadaRepository;
    private final EmailService emailService;

    public AnticipoService(SolicitudAnticipoRepository solicitudRepo,
                           VistaControlSaldosRepository saldosRepo,
                           UsuarioRepository usuarioRepository,
                           TemporadaRepository temporadaRepository,
                           EmailService emailService) {
        this.solicitudRepo = solicitudRepo;
        this.saldosRepo = saldosRepo;
        this.usuarioRepository = usuarioRepository;
        this.temporadaRepository = temporadaRepository;
        this.emailService = emailService;
    }

    /**
     * Crea una nueva solicitud de anticipo aplicando la validación automática de saldo.
     *
     * <p><strong>Flujo completo:</strong></p>
     * <ol>
     *   <li>Extrae el usuario autenticado del token JWT (vía {@link SecurityContextHolder}).</li>
     *   <li>Obtiene la temporada activa desde BD ({@code es_actual = true}).</li>
     *   <li>Consulta la vista {@code vista_control_saldos_enl} para obtener el saldo
     *       disponible del equipo del usuario en la temporada activa.</li>
     *   <li>Compara el monto solicitado contra el saldo del rubro correspondiente:
     *       <ul>
     *         <li>Si <strong>excede</strong>: guarda como {@code RECHAZADO} y notifica al solicitante.</li>
     *         <li>Si <strong>cabe</strong>: guarda como {@code PENDIENTE} y notifica al solicitante
     *             y al coordinador ({@code ENL_RECURSOS}).</li>
     *       </ul>
     *   </li>
     *   <li>Los correos se envían después del commit de la transacción.</li>
     * </ol>
     *
     * @param request datos de la solicitud (título, descripción, monto, tipo de presupuesto)
     * @return DTO con el ID, estado y mensaje descriptivo del resultado
     * @throws IllegalStateException    si no hay usuario autenticado o no hay temporada activa
     * @throws IllegalArgumentException si no hay presupuesto configurado para el equipo y temporada
     */
    @Transactional
    public AnticipoResponse crearSolicitud(AnticipoRequest request) {
        // Paso 1: identificar quién está creando la solicitud desde la sesión activa
        String emailAutenticado = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario solicitante = usuarioRepository.findByEmail(emailAutenticado)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado."));

        // Paso 2: obtener la temporada activa automáticamente (sin que el front la envíe)
        Temporada temporada = temporadaRepository.findByEsActualTrue()
                .orElseThrow(() -> new IllegalStateException("No hay una temporada activa configurada."));

        // Paso 3: consultar saldo disponible en la vista de control
        VistaControlSaldos saldos = saldosRepo.findById(
                new VistaControlSaldosId(solicitante.getEquipo().getId(), temporada.getId())
        ).orElseThrow(() -> new IllegalArgumentException(
                "No se encontró presupuesto para el equipo y temporada activa."));

        // Paso 4: seleccionar el saldo según el rubro solicitado
        BigDecimal saldoDisponible = request.tipoPresupuesto() == TipoPresupuesto.ENTRENAMIENTO
                ? saldos.getSaldoEntrenamiento()
                : saldos.getSaldoMentoreo();

        // Construir la entidad con los datos de la sesión
        SolicitudAnticipo solicitud = new SolicitudAnticipo();
        solicitud.setTitulo(request.titulo());
        solicitud.setDescripcion(request.descripcion());
        solicitud.setMontoSolicitado(request.montoSolicitado());
        solicitud.setTipoPresupuesto(request.tipoPresupuesto());
        solicitud.setEquipoId(solicitante.getEquipo().getId());
        solicitud.setTemporadaId(temporada.getId());
        solicitud.setUsuarioId(solicitante.getId());

        // Paso 5A: rechazo automático por saldo insuficiente
        if (request.montoSolicitado().compareTo(saldoDisponible) > 0) {
            solicitud.setEstado(EstadoSolicitud.RECHAZADO);
            solicitud.setMotivoRechazo("Sistema: Monto excede el saldo disponible");
            solicitudRepo.save(solicitud);

            String mensaje = String.format(
                    "Rechazo automático: El monto solicitado supera el saldo disponible ($%,.0f) en la bolsa de %s.",
                    saldoDisponible, request.tipoPresupuesto().name());

            // Capturar variables para el hilo asíncrono (las lambdas requieren variables efectivamente finales)
            String emailSolicitante = solicitante.getEmail();
            String nombreSolicitante = solicitante.getName();
            String tituloSolicitud = request.titulo();

            // Correo al solicitante: se envía después del commit de la transacción
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailService.enviar(
                            emailSolicitante,
                            "SIGLOCC - Solicitud rechazada: " + tituloSolicitud,
                            String.format("Hola %s,\n\nTu solicitud '%s' ha sido rechazada automáticamente.\n\nMotivo: %s\n\nSaludos,\nSIGLOCC",
                                    nombreSolicitante, tituloSolicitud, mensaje)
                    );
                }
            });

            return new AnticipoResponse(solicitud.getId(), EstadoSolicitud.RECHAZADO.name(), mensaje);
        }

        // Paso 5B: saldo suficiente → guardar como PENDIENTE
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitudRepo.save(solicitud);

        // Capturar todos los datos necesarios antes de que la transacción cierre el contexto JPA
        String emailSolicitante = solicitante.getEmail();
        String nombreSolicitante = solicitante.getName();
        String apellidoSolicitante = solicitante.getLastname();
        String tituloSolicitud = request.titulo();
        BigDecimal monto = request.montoSolicitado();
        String tipo = request.tipoPresupuesto().name();
        String emailAprobador = usuarioRepository.findFirstByRol_Name("ENL_RECURSOS")
                .map(Usuario::getEmail).orElse(null);
        String nombreAprobador = usuarioRepository.findFirstByRol_Name("ENL_RECURSOS")
                .map(Usuario::getName).orElse(null);

        // Correos al solicitante y al aprobador: se envían DESPUÉS del commit exitoso
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Notificar al solicitante que su solicitud fue recibida
                emailService.enviar(
                        emailSolicitante,
                        "SIGLOCC - Solicitud recibida: " + tituloSolicitud,
                        String.format("Hola %s,\n\nTu solicitud '%s' por $%,.0f ha sido recibida y está pendiente de aprobación.\n\nSaludos,\nSIGLOCC",
                                nombreSolicitante, tituloSolicitud, monto)
                );

                // Notificar al coordinador ENL_RECURSOS para que gestione la aprobación
                if (emailAprobador != null) {
                    emailService.enviar(
                            emailAprobador,
                            "SIGLOCC - Nueva solicitud pendiente: " + tituloSolicitud,
                            String.format("Hola %s,\n\nHay una nueva solicitud de anticipo pendiente de tu aprobación.\n\nSolicitante: %s %s\nTítulo: %s\nMonto: $%,.0f\nTipo: %s\n\nIngresa al sistema para aprobar o rechazar.\n\nSaludos,\nSIGLOCC",
                                    nombreAprobador, nombreSolicitante, apellidoSolicitante,
                                    tituloSolicitud, monto, tipo)
                    );
                }
            }
        });

        return new AnticipoResponse(
                solicitud.getId(),
                EstadoSolicitud.PENDIENTE.name(),
                "Solicitud enviada correctamente y en espera de aprobación del ENL."
        );
    }

    /**
     * Aprueba una solicitud de anticipo que esté en estado {@code PENDIENTE}.
     *
     * <p>Solo el rol {@code ENL_RECURSOS} puede invocar este método (restricción
     * aplicada en {@link com.siglocc.controller.AnticipoController} con
     * {@code @PreAuthorize}).</p>
     *
     * <p>Al cambiar el estado a {@code APROBADO}, la vista
     * {@code vista_control_saldos_enl} actualizará automáticamente el monto
     * ejecutado en la próxima consulta, ya que la vista suma los anticipos
     * aprobados en tiempo real.</p>
     *
     * @param id ID de la solicitud a aprobar
     * @return DTO con el estado actualizado y mensaje de confirmación
     * @throws IllegalArgumentException si no existe una solicitud con ese ID
     * @throws IllegalStateException    si la solicitud no está en estado {@code PENDIENTE}
     */
    @Transactional
    public AnticipoResponse aprobarSolicitud(Integer id) {
        SolicitudAnticipo solicitud = solicitudRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada con id: " + id));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden aprobar solicitudes en estado PENDIENTE.");
        }

        solicitud.setEstado(EstadoSolicitud.APROBADO);
        solicitud.setFechaAprobacionFinal(LocalDateTime.now());
        solicitudRepo.save(solicitud);

        // Capturar datos antes de que la transacción libere el contexto JPA
        String tituloSolicitud = solicitud.getTitulo();
        BigDecimal monto = solicitud.getMontoSolicitado();
        LocalDateTime fechaAprobacion = solicitud.getFechaAprobacionFinal();
        Integer usuarioId = solicitud.getUsuarioId();

        // Correo al solicitante: se envía después del commit exitoso
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                usuarioRepository.findById(usuarioId).ifPresent(solicitante ->
                        emailService.enviar(
                                solicitante.getEmail(),
                                "SIGLOCC - Solicitud aprobada: " + tituloSolicitud,
                                String.format("Hola %s,\n\nTu solicitud '%s' por $%,.0f ha sido aprobada.\n\nFecha de aprobación: %s\n\nSaludos,\nSIGLOCC",
                                        solicitante.getName(), tituloSolicitud, monto, fechaAprobacion)
                        )
                );
            }
        });

        return new AnticipoResponse(solicitud.getId(), EstadoSolicitud.APROBADO.name(), "Solicitud aprobada exitosamente.");
    }
}
