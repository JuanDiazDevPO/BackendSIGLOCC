package com.siglocc.service;

import com.siglocc.dto.AnticipoRequest;
import com.siglocc.dto.AnticipoResponse;
import com.siglocc.entity.*;
import com.siglocc.repository.SolicitudAnticipoRepository;
import com.siglocc.repository.TemporadaRepository;
import com.siglocc.repository.UsuarioRepository;
import com.siglocc.repository.VistaControlSaldosRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Servicio que contiene la lógica de negocio del módulo de anticipos.
 *
 * <p>Implementa dos operaciones principales:</p>
 * <ul>
 *   <li>{@link #crearSolicitud} – valida saldo, persiste la solicitud,
 *       genera el PDF formal y notifica por correo</li>
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

    private static final Logger log = LoggerFactory.getLogger(AnticipoService.class);

    private final SolicitudAnticipoRepository solicitudRepo;
    private final VistaControlSaldosRepository saldosRepo;
    private final UsuarioRepository usuarioRepository;
    private final TemporadaRepository temporadaRepository;
    private final EmailService emailService;
    private final AnticipoDocumentoService documentoService;
    private final StorageService storageService;

    public AnticipoService(SolicitudAnticipoRepository solicitudRepo,
                           VistaControlSaldosRepository saldosRepo,
                           UsuarioRepository usuarioRepository,
                           TemporadaRepository temporadaRepository,
                           EmailService emailService,
                           AnticipoDocumentoService documentoService,
                           StorageService storageService) {
        this.solicitudRepo    = solicitudRepo;
        this.saldosRepo       = saldosRepo;
        this.usuarioRepository = usuarioRepository;
        this.temporadaRepository = temporadaRepository;
        this.emailService     = emailService;
        this.documentoService = documentoService;
        this.storageService   = storageService;
    }

    /**
     * Crea una nueva solicitud de anticipo aplicando la validación automática de saldo.
     *
     * <p><strong>Flujo completo:</strong></p>
     * <ol>
     *   <li>Extrae el usuario autenticado del token JWT.</li>
     *   <li>Obtiene la temporada activa desde BD.</li>
     *   <li>Consulta la vista de control de saldos.</li>
     *   <li>Si el monto excede el saldo: guarda como RECHAZADO y notifica.</li>
     *   <li>Si el monto cabe: guarda como PENDIENTE y notifica a solicitante y aprobador.</li>
     *   <li>En ambos casos genera el PDF formal y lo almacena en disco.</li>
     * </ol>
     *
     * @param request datos del formulario de solicitud
     * @return DTO con ID, estado, mensaje y ruta del PDF generado
     */
    @Transactional
    public AnticipoResponse crearSolicitud(AnticipoRequest request) {
        // Paso 1: identificar quién está creando la solicitud
        String emailAutenticado = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario solicitante = usuarioRepository.findByEmail(emailAutenticado)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado."));

        // Paso 2: obtener la temporada activa
        Temporada temporada = temporadaRepository.findByEsActualTrue()
                .orElseThrow(() -> new IllegalStateException("No hay una temporada activa configurada."));

        // Paso 3: consultar saldo disponible
        VistaControlSaldos saldos = saldosRepo.findById(
                new VistaControlSaldosId(solicitante.getEquipo().getId(), temporada.getId())
        ).orElseThrow(() -> new IllegalArgumentException(
                "No se encontró presupuesto para el equipo y temporada activa."));

        // Paso 4: seleccionar el saldo según el rubro
        BigDecimal saldoDisponible = request.tipoPresupuesto() == TipoPresupuesto.ENTRENAMIENTO
                ? saldos.getSaldoEntrenamiento()
                : saldos.getSaldoMentoreo();

        // Construir la entidad con todos los campos
        SolicitudAnticipo solicitud = new SolicitudAnticipo();
        solicitud.setTitulo(request.titulo());
        solicitud.setDescripcion(request.descripcion());
        solicitud.setMontoSolicitado(request.montoSolicitado());
        solicitud.setTipoPresupuesto(request.tipoPresupuesto());
        solicitud.setCiudad(request.ciudad());
        solicitud.setCedula(request.cedula());
        solicitud.setBanco(request.banco());
        solicitud.setTipoCuenta(request.tipoCuenta());
        solicitud.setNumeroCuenta(request.numeroCuenta());
        solicitud.setNombreTitular(request.nombreTitular());
        solicitud.setCedulaTitular(request.cedulaTitular());
        solicitud.setEquipoId(solicitante.getEquipo().getId());
        solicitud.setTemporadaId(temporada.getId());
        solicitud.setUsuarioId(solicitante.getId());

        // Paso 5A: rechazo automático por saldo insuficiente
        if (request.montoSolicitado().compareTo(saldoDisponible) > 0) {
            solicitud.setEstado(EstadoSolicitud.RECHAZADO);
            solicitud.setMotivoRechazo("Sistema: Monto excede el saldo disponible");
            solicitudRepo.save(solicitud);

            String rutaPdf = generarYAlmacenarPdf(solicitud, solicitante);

            String mensaje = String.format(
                    "Rechazo automático: El monto solicitado supera el saldo disponible ($%,.0f) en la bolsa de %s.",
                    saldoDisponible, request.tipoPresupuesto().name());

            String emailSolicitante = solicitante.getEmail();
            String nombreSolicitante = solicitante.getName();
            String tituloSolicitud = request.titulo();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailService.enviarHtml(
                            emailSolicitante,
                            "SIGLOCC - Solicitud rechazada: " + tituloSolicitud,
                            "solicitud-rechazada",
                            Map.of(
                                "nombreSolicitante", nombreSolicitante,
                                "tituloSolicitud", tituloSolicitud,
                                "motivo", mensaje
                            )
                    );
                }
            });

            return new AnticipoResponse(solicitud.getId(), EstadoSolicitud.RECHAZADO.name(),
                    mensaje, rutaPdf);
        }

        // Paso 5B: saldo suficiente → guardar como PENDIENTE
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitudRepo.save(solicitud);

        String rutaPdf = generarYAlmacenarPdf(solicitud, solicitante);

        String emailSolicitante  = solicitante.getEmail();
        String nombreSolicitante = solicitante.getName();
        String apellidoSolicitante = solicitante.getLastname();
        String tituloSolicitud   = request.titulo();
        BigDecimal monto         = request.montoSolicitado();
        String tipo              = request.tipoPresupuesto().name();
        String emailAprobador    = usuarioRepository.findFirstByRol_Name("ENL_RECURSOS")
                .map(Usuario::getEmail).orElse(null);
        String nombreAprobador   = usuarioRepository.findFirstByRol_Name("ENL_RECURSOS")
                .map(Usuario::getName).orElse(null);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emailService.enviarHtml(
                        emailSolicitante,
                        "SIGLOCC - Solicitud recibida: " + tituloSolicitud,
                        "solicitud-recibida",
                        Map.of(
                            "nombreSolicitante", nombreSolicitante,
                            "tituloSolicitud", tituloSolicitud,
                            "monto", monto
                        )
                );

                if (emailAprobador != null) {
                    emailService.enviarHtml(
                            emailAprobador,
                            "SIGLOCC - Nueva solicitud pendiente: " + tituloSolicitud,
                            "nueva-solicitud-aprobador",
                            Map.of(
                                "nombreAprobador", nombreAprobador,
                                "nombreSolicitante", nombreSolicitante,
                                "apellidoSolicitante", apellidoSolicitante,
                                "tituloSolicitud", tituloSolicitud,
                                "monto", monto,
                                "tipo", tipo
                            )
                    );
                }
            }
        });

        return new AnticipoResponse(
                solicitud.getId(),
                EstadoSolicitud.PENDIENTE.name(),
                "Solicitud enviada correctamente y en espera de aprobación del ENL.",
                rutaPdf
        );
    }

    /**
     * Aprueba una solicitud de anticipo que esté en estado {@code PENDIENTE}.
     *
     * @param id ID de la solicitud a aprobar
     * @return DTO con el estado actualizado y mensaje de confirmación
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

        String tituloSolicitud  = solicitud.getTitulo();
        BigDecimal monto        = solicitud.getMontoSolicitado();
        LocalDateTime fechaAprobacion = solicitud.getFechaAprobacionFinal();
        Integer usuarioId       = solicitud.getUsuarioId();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                usuarioRepository.findById(usuarioId).ifPresent(solicitante ->
                        emailService.enviarHtml(
                                solicitante.getEmail(),
                                "SIGLOCC - Solicitud aprobada: " + tituloSolicitud,
                                "solicitud-aprobada",
                                Map.of(
                                    "nombreSolicitante", solicitante.getName(),
                                    "tituloSolicitud", tituloSolicitud,
                                    "monto", monto,
                                    "fechaAprobacion", fechaAprobacion
                                )
                        )
                );
            }
        });

        return new AnticipoResponse(solicitud.getId(), EstadoSolicitud.APROBADO.name(),
                "Solicitud aprobada exitosamente.", solicitud.getRutaPdf());
    }

    // ── Privados ──────────────────────────────────────────────────────────

    /**
     * Genera el PDF del anticipo, lo almacena en disco y actualiza la entidad con la ruta.
     *
     * @return ruta relativa del PDF, o {@code null} si la generación falló
     */
    private String generarYAlmacenarPdf(SolicitudAnticipo solicitud, Usuario solicitante) {
        try {
            String cargo = mapearCargo(solicitante.getRol().getName());
            byte[] pdfBytes = documentoService.generarPdf(
                    solicitud,
                    solicitante.getName(),
                    solicitante.getLastname(),
                    cargo,
                    solicitante.getEquipo().getNombre()
            );
            String ruta = storageService.almacenarPdfAnticipo(pdfBytes, solicitud.getId());
            solicitud.setRutaPdf(ruta);
            solicitudRepo.save(solicitud);
            return ruta;
        } catch (Exception e) {
            log.error("No se pudo generar el PDF del anticipo {}: {}", solicitud.getId(), e.getMessage());
            return null;
        }
    }

    private String mapearCargo(String rolNombre) {
        if (rolNombre == null) return "Coordinador";
        return switch (rolNombre) {
            case "ENL"          -> "Coordinador Nacional de Liderazgo";
            case "ENL_RECURSOS" -> "Coordinador Nacional de Finanzas";
            case "ERLE"         -> "Coordinador Regional de Liderazgo";
            case "ERL"          -> "Coordinador Local de Recursos";
            default             -> rolNombre;
        };
    }
}
