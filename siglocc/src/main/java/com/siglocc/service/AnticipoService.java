package com.siglocc.service;

import com.siglocc.dto.AnticipoDetalleResponse;
import com.siglocc.dto.AnticipoRequest;
import com.siglocc.dto.AnticipoResponse;
import com.siglocc.dto.CambiarEstadoRequest;
import com.siglocc.dto.SaldosEquipoResponse;
import com.siglocc.entity.*;
import com.siglocc.repository.EquipoRepository;
import com.siglocc.repository.SolicitudAnticipoRepository;
import com.siglocc.repository.TemporadaRepository;
import com.siglocc.repository.UsuarioRepository;
import com.siglocc.repository.VistaControlSaldosRepository;
import com.siglocc.security.IdentidadJwtException;
import com.siglocc.security.JwtAuthDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Servicio que contiene la lógica de negocio del módulo de anticipos.
 *
 * <p>Implementa el ciclo de vida completo de una solicitud:</p>
 * <ul>
 *   <li>{@link #crearSolicitud} – valida saldo, persiste la solicitud con el estado
 *       inicial que corresponda según el tipo de equipo, genera el PDF formal y notifica
 *       por correo al siguiente aprobador</li>
 *   <li>{@link #cambiarEstado} – permite a ERLE_RECURSOS y ENL_RECURSOS aprobar o rechazar
 *       cada paso del flujo, registrando quién firmó (ver {@link EstadoSolicitud})</li>
 *   <li>{@link #listarSolicitudes} – lista solicitudes filtrando por la jerarquía del
 *       usuario autenticado, igual que {@code ReporteService.listarReportes}</li>
 * </ul>
 *
 * <p><strong>Flujo de aprobación (mismo patrón que {@code ReporteService}):</strong>
 * un equipo ERL pasa primero por su ERLE y luego por el ENL; un equipo ERLE salta
 * directo al ENL; una solicitud del propio equipo ENL queda autoaprobada de inmediato
 * porque no hay nadie por encima que deba revisarla.</p>
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
    private final EquipoRepository equipoRepository;
    private final EmailService emailService;
    private final AnticipoDocumentoService documentoService;
    private final StorageService storageService;

    public AnticipoService(SolicitudAnticipoRepository solicitudRepo,
                           VistaControlSaldosRepository saldosRepo,
                           UsuarioRepository usuarioRepository,
                           TemporadaRepository temporadaRepository,
                           EquipoRepository equipoRepository,
                           EmailService emailService,
                           AnticipoDocumentoService documentoService,
                           StorageService storageService) {
        this.solicitudRepo    = solicitudRepo;
        this.saldosRepo       = saldosRepo;
        this.usuarioRepository = usuarioRepository;
        this.temporadaRepository = temporadaRepository;
        this.equipoRepository  = equipoRepository;
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
            solicitud.setFechaRechazo(LocalDateTime.now());
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

        // Paso 5B: saldo suficiente → el siguiente estado depende del tipo de equipo
        // solicitante. ERL necesita el visto bueno de su ERLE y luego del ENL; un
        // ERLE salta directo a PENDIENTE_ENL; el ENL no tiene a nadie por encima,
        // así que su propia solicitud queda aprobada de inmediato.
        TipoEquipo tipoEquipo = solicitante.getEquipo().getTipo();
        switch (tipoEquipo) {
            case ERL -> solicitud.setEstado(EstadoSolicitud.PENDIENTE_ERLE);
            case ERLE -> solicitud.setEstado(EstadoSolicitud.PENDIENTE_ENL);
            case ENL -> {
                solicitud.setEstado(EstadoSolicitud.APROBADO);
                solicitud.setFechaAprobacionFinal(LocalDateTime.now());
                solicitud.setAprobadorEnlId(solicitante.getId());
            }
        }
        solicitudRepo.save(solicitud);

        String rutaPdf = generarYAlmacenarPdf(solicitud, solicitante);

        String emailSolicitante    = solicitante.getEmail();
        String nombreSolicitante   = solicitante.getName();
        String apellidoSolicitante = solicitante.getLastname();
        String tituloSolicitud     = request.titulo();
        BigDecimal monto           = request.montoSolicitado();
        String tipo                = request.tipoPresupuesto().name();
        LocalDateTime fechaAprobacion = solicitud.getFechaAprobacionFinal();

        if (tipoEquipo == TipoEquipo.ENL) {
            // Autoaprobada: se notifica directamente al solicitante, sin aprobador pendiente.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailService.enviarHtml(
                            emailSolicitante,
                            "SIGLOCC - Solicitud aprobada: " + tituloSolicitud,
                            "solicitud-aprobada",
                            Map.of(
                                "nombreSolicitante", nombreSolicitante,
                                "tituloSolicitud", tituloSolicitud,
                                "monto", monto,
                                "fechaAprobacion", fechaAprobacion
                            )
                    );
                }
            });

            return new AnticipoResponse(solicitud.getId(), EstadoSolicitud.APROBADO.name(),
                    "Solicitud aprobada automáticamente (equipo ENL).", rutaPdf);
        }

        // ERL notifica a su ERLE; ERLE notifica directo al ENL.
        Usuario aprobador = tipoEquipo == TipoEquipo.ERL
                ? resolverCoordinadorErle(solicitante.getEquipo().getErleId())
                : usuarioRepository.findFirstByRol_Name("ENL_RECURSOS").orElse(null);
        String emailAprobador  = aprobador != null ? aprobador.getEmail() : null;
        String nombreAprobador = aprobador != null ? aprobador.getName() : null;
        String mensaje = tipoEquipo == TipoEquipo.ERL
                ? "Solicitud enviada correctamente y en espera de aprobación del ERLE."
                : "Solicitud enviada correctamente y en espera de aprobación del ENL.";

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

        return new AnticipoResponse(solicitud.getId(), solicitud.getEstado().name(), mensaje, rutaPdf);
    }

    /**
     * Busca el coordinador {@code ERLE_RECURSOS} del clúster al que pertenece un equipo ERL.
     *
     * @param erleEquipoId ID del equipo ERLE (padre del ERL solicitante), puede ser {@code null}
     * @return el usuario encontrado, o {@code null} si no hay coordinador configurado
     */
    private Usuario resolverCoordinadorErle(Integer erleEquipoId) {
        if (erleEquipoId == null) return null;
        return usuarioRepository.findFirstByEquipo_IdAndRol_Name(erleEquipoId, "ERLE_RECURSOS")
                .orElse(null);
    }

    /**
     * Cambia el estado de una solicitud de anticipo y registra la auditoría del aprobador.
     *
     * <p><strong>Transiciones permitidas:</strong></p>
     * <ul>
     *   <li><strong>ERLE:</strong> {@code PENDIENTE_ERLE → PENDIENTE_ENL} o {@code RECHAZADO},
     *       solo sobre solicitudes de equipos de su propio clúster.</li>
     *   <li><strong>ENL:</strong> {@code PENDIENTE_ENL → APROBADO} o {@code RECHAZADO}.</li>
     * </ul>
     *
     * @param id      ID de la solicitud a modificar
     * @param request nuevo estado y observaciones (obligatorias al rechazar)
     * @return DTO con el estado actualizado y mensaje de confirmación
     * @throws IllegalArgumentException si la solicitud no existe, el estado destino es inválido
     *                                   o faltan observaciones al rechazar
     * @throws IllegalStateException    si el tipo de equipo no tiene permisos para esa transición,
     *                                   o si el ERLE intenta actuar sobre una solicitud fuera de su clúster
     */
    @Transactional
    public AnticipoResponse cambiarEstado(Integer id, CambiarEstadoRequest request) {
        JwtAuthDetails details = obtenerDetails();
        Integer equipoIdRevisor = details.equipoId();
        String equipoTipo       = details.equipoTipo();

        SolicitudAnticipo solicitud = solicitudRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada con id: " + id));

        EstadoSolicitud nuevoEstado;
        try {
            nuevoEstado = EstadoSolicitud.valueOf(request.nuevoEstado());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Estado no válido: '" + request.nuevoEstado() +
                    "'. Valores permitidos: PENDIENTE_ENL, APROBADO, RECHAZADO.");
        }

        validarTransicionYPropiedad(solicitud, nuevoEstado, equipoTipo, equipoIdRevisor);

        Integer revisorId = resolverUsuarioId();

        if (nuevoEstado == EstadoSolicitud.RECHAZADO) {
            if (request.observaciones() == null || request.observaciones().isBlank()) {
                throw new IllegalArgumentException(
                        "El campo 'observaciones' es obligatorio al rechazar una solicitud.");
            }
            solicitud.setMotivoRechazo(request.observaciones());
            solicitud.setFechaRechazo(LocalDateTime.now());
            solicitud.setRechazadoPorId(revisorId);
        }

        if ("ERLE".equals(equipoTipo)) {
            solicitud.setAprobadorErleId(revisorId);
            if (nuevoEstado == EstadoSolicitud.PENDIENTE_ENL) {
                solicitud.setFechaAprobacionErle(LocalDateTime.now());
            }
        } else if ("ENL".equals(equipoTipo)) {
            solicitud.setAprobadorEnlId(revisorId);
            if (nuevoEstado == EstadoSolicitud.APROBADO) {
                solicitud.setFechaAprobacionFinal(LocalDateTime.now());
            }
        }

        solicitud.setEstado(nuevoEstado);
        solicitudRepo.save(solicitud);

        notificarCambioEstado(solicitud, nuevoEstado, request.observaciones());

        String mensaje = switch (nuevoEstado) {
            case PENDIENTE_ENL -> "Solicitud aprobada por el ERLE y enviada al ENL para aprobación final.";
            case APROBADO      -> "Solicitud aprobada exitosamente.";
            case RECHAZADO     -> "Solicitud rechazada.";
            default            -> "Solicitud actualizada.";
        };
        return new AnticipoResponse(solicitud.getId(), nuevoEstado.name(), mensaje, solicitud.getRutaPdf());
    }

    /**
     * Valida que la transición de estado sea permitida para el tipo de equipo que
     * realiza la acción, y que el ERLE solo actúe sobre solicitudes de su propio clúster.
     *
     * @throws IllegalStateException    si el estado actual no corresponde al paso del revisor,
     *                                   o el ERLE no es dueño del clúster de la solicitud
     * @throws IllegalArgumentException si el estado destino no está permitido para ese tipo
     */
    private void validarTransicionYPropiedad(SolicitudAnticipo solicitud, EstadoSolicitud nuevo,
                                              String equipoTipo, Integer equipoIdRevisor) {
        switch (equipoTipo) {
            case "ERLE" -> {
                if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE_ERLE) {
                    throw new IllegalStateException(
                            "El ERLE solo puede gestionar solicitudes en estado PENDIENTE_ERLE. " +
                            "Estado actual: " + solicitud.getEstado().name());
                }
                if (nuevo != EstadoSolicitud.PENDIENTE_ENL && nuevo != EstadoSolicitud.RECHAZADO) {
                    throw new IllegalArgumentException(
                            "El ERLE solo puede mover solicitudes a PENDIENTE_ENL o RECHAZADO.");
                }
                Equipo equipoSolicitante = equipoRepository.findById(solicitud.getEquipoId())
                        .orElseThrow(() -> new IllegalStateException(
                                "El equipo solicitante " + solicitud.getEquipoId() + " ya no existe."));
                if (!equipoIdRevisor.equals(equipoSolicitante.getErleId())) {
                    throw new IllegalStateException(
                            "No tiene permisos sobre esta solicitud: el equipo solicitante " +
                            "no pertenece a su clúster.");
                }
            }
            case "ENL" -> {
                if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE_ENL) {
                    throw new IllegalStateException(
                            "El ENL solo puede gestionar solicitudes en estado PENDIENTE_ENL. " +
                            "Estado actual: " + solicitud.getEstado().name());
                }
                if (nuevo != EstadoSolicitud.APROBADO && nuevo != EstadoSolicitud.RECHAZADO) {
                    throw new IllegalArgumentException(
                            "El ENL solo puede mover solicitudes a APROBADO o RECHAZADO.");
                }
            }
            default -> throw new IllegalStateException(
                    "El tipo de equipo '" + equipoTipo + "' no tiene permisos para cambiar " +
                    "el estado de solicitudes de anticipo.");
        }
    }

    /**
     * Registra el envío de correo correspondiente al nuevo estado de una solicitud.
     *
     * <ul>
     *   <li>{@code PENDIENTE_ENL} – notifica al ENL_RECURSOS que hay una nueva solicitud a revisar.</li>
     *   <li>{@code APROBADO} – notifica al solicitante que su anticipo quedó aprobado.</li>
     *   <li>{@code RECHAZADO} – notifica al solicitante con el motivo del rechazo.</li>
     * </ul>
     */
    private void notificarCambioEstado(SolicitudAnticipo solicitud, EstadoSolicitud nuevoEstado,
                                       String observaciones) {
        Integer usuarioId      = solicitud.getUsuarioId();
        String tituloSolicitud = solicitud.getTitulo();
        BigDecimal monto       = solicitud.getMontoSolicitado();

        if (nuevoEstado == EstadoSolicitud.PENDIENTE_ENL) {
            Usuario aprobadorEnl = usuarioRepository.findFirstByRol_Name("ENL_RECURSOS").orElse(null);
            if (aprobadorEnl == null) return;
            String emailAprobador  = aprobadorEnl.getEmail();
            String nombreAprobador = aprobadorEnl.getName();
            usuarioRepository.findById(usuarioId).ifPresent(solicitante -> {
                String nombreSolicitante   = solicitante.getName();
                String apellidoSolicitante = solicitante.getLastname();
                String tipo = solicitud.getTipoPresupuesto() != null ? solicitud.getTipoPresupuesto().name() : "";
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
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
                });
            });
            return;
        }

        if (nuevoEstado == EstadoSolicitud.APROBADO) {
            LocalDateTime fechaAprobacion = solicitud.getFechaAprobacionFinal();
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
            return;
        }

        if (nuevoEstado == EstadoSolicitud.RECHAZADO) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    usuarioRepository.findById(usuarioId).ifPresent(solicitante ->
                            emailService.enviarHtml(
                                    solicitante.getEmail(),
                                    "SIGLOCC - Solicitud rechazada: " + tituloSolicitud,
                                    "solicitud-rechazada",
                                    Map.of(
                                        "nombreSolicitante", solicitante.getName(),
                                        "tituloSolicitud", tituloSolicitud,
                                        "motivo", observaciones
                                    )
                            )
                    );
                }
            });
        }
    }

    /**
     * Devuelve los saldos presupuestales del equipo del usuario autenticado
     * en la temporada activa.
     *
     * <p>El {@code equipoId} se extrae de {@link JwtAuthDetails} en el
     * {@code SecurityContextHolder}; nunca proviene del cliente.</p>
     *
     * @return DTO con presupuesto, ejecutado y disponible por rubro
     * @throws IdentidadJwtException    si el token no contiene identidad jerárquica (→ 400)
     * @throws IllegalStateException    si no hay temporada activa (→ 409)
     * @throws NoSuchElementException   si el equipo no tiene presupuesto configurado (→ 404)
     */
    public SaldosEquipoResponse consultarMisSaldos() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth.getDetails() instanceof JwtAuthDetails details)) {
            throw new IdentidadJwtException(
                    "El token no contiene identidad jerárquica válida. Vuelve a iniciar sesión.");
        }
        Integer equipoId = details.equipoId();

        Temporada temporada = temporadaRepository.findByEsActualTrue()
                .orElseThrow(() -> new IllegalStateException("No hay una temporada activa configurada."));

        VistaControlSaldos saldos = saldosRepo.findById(
                new VistaControlSaldosId(equipoId, temporada.getId())
        ).orElseThrow(() -> new NoSuchElementException(
                "No hay presupuesto configurado para el equipo en la temporada activa."));

        String nombreEquipo = equipoRepository.findById(equipoId)
                .map(Equipo::getNombre)
                .orElse("Equipo " + equipoId);

        SaldosEquipoResponse.RubroSaldo mentoreo = saldos.getPresupuestoMentoreo() != null
                ? new SaldosEquipoResponse.RubroSaldo(
                        saldos.getPresupuestoMentoreo(),
                        saldos.getEjecutadoMentoreo() != null ? saldos.getEjecutadoMentoreo() : BigDecimal.ZERO,
                        saldos.getSaldoMentoreo())
                : null;

        return new SaldosEquipoResponse(
                equipoId,
                nombreEquipo,
                temporada.getId(),
                new SaldosEquipoResponse.RubroSaldo(
                        saldos.getPresupuestoEntrenamiento(),
                        saldos.getEjecutadoEntrenamiento() != null ? saldos.getEjecutadoEntrenamiento() : BigDecimal.ZERO,
                        saldos.getSaldoEntrenamiento()
                ),
                mentoreo
        );
    }

    /**
     * Retorna las solicitudes de anticipo visibles para el usuario autenticado,
     * de la más reciente a la más antigua, con filtrado jerárquico automático.
     *
     * <ul>
     *   <li><strong>ENL:</strong> todas las solicitudes, opcionalmente filtradas por temporada.</li>
     *   <li><strong>ERLE:</strong> su propio equipo más todos los ERL de su clúster.</li>
     * </ul>
     *
     * @param temporadaId ID de la temporada para filtrar, o {@code null} para no filtrar
     * @return lista de solicitudes visibles con todos sus campos
     * @throws IllegalStateException si el tipo de equipo no tiene acceso a este listado
     */
    public List<AnticipoDetalleResponse> listarSolicitudes(Integer temporadaId) {
        JwtAuthDetails details = obtenerDetails();
        Integer equipoId  = details.equipoId();
        String equipoTipo = details.equipoTipo();

        List<SolicitudAnticipo> solicitudes = switch (equipoTipo) {
            case "ENL"  -> solicitudRepo.findAllOpcionalmentePorTemporada(temporadaId);
            case "ERLE" -> solicitudRepo.findByErleClusterOpcionalmentePorTemporada(equipoId, temporadaId);
            default -> throw new IllegalStateException(
                    "El tipo de equipo '" + equipoTipo + "' no tiene acceso al listado de anticipos.");
        };

        return solicitudes.stream()
                .map(this::toDetalleResponse)
                .toList();
    }

    /**
     * Convierte una entidad {@link SolicitudAnticipo} en su DTO de detalle completo.
     *
     * @param s entidad persistida
     * @return DTO listo para serializar en la respuesta HTTP
     */
    private AnticipoDetalleResponse toDetalleResponse(SolicitudAnticipo s) {
        String equipoNombre = equipoRepository.findById(s.getEquipoId())
                .map(Equipo::getNombre)
                .orElse(null);

        return new AnticipoDetalleResponse(
                s.getId(),
                s.getTitulo(),
                s.getDescripcion(),
                s.getMontoSolicitado(),
                s.getTipoPresupuesto() != null ? s.getTipoPresupuesto().name() : null,
                s.getEstado() != null ? s.getEstado().name() : null,
                s.getMotivoRechazo(),
                s.getCiudad(),
                s.getCedula(),
                s.getBanco(),
                s.getTipoCuenta() != null ? s.getTipoCuenta().name() : null,
                s.getNumeroCuenta(),
                s.getNombreTitular(),
                s.getCedulaTitular(),
                s.getRutaPdf(),
                s.getEquipoId(),
                equipoNombre,
                s.getTemporadaId(),
                s.getUsuarioId(),
                s.getFechaSolicitud(),
                s.getAprobadorErleId(),
                s.getFechaAprobacionErle(),
                s.getAprobadorEnlId(),
                s.getFechaAprobacionFinal(),
                s.getRechazadoPorId(),
                s.getFechaRechazo()
        );
    }

    /**
     * Extrae el {@link JwtAuthDetails} del contexto de seguridad activo.
     *
     * @return detalles de identidad jerárquica del usuario autenticado
     * @throws IdentidadJwtException si el token no contiene identidad jerárquica válida
     */
    private JwtAuthDetails obtenerDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth.getDetails() instanceof JwtAuthDetails details)) {
            throw new IdentidadJwtException(
                    "El token no contiene identidad jerárquica válida (equipoId/equipoTipo). Vuelve a iniciar sesión.");
        }
        return details;
    }

    /**
     * Resuelve el ID del usuario autenticado buscando por el email del token JWT.
     *
     * @return ID del usuario autenticado, o {@code null} si no se encuentra
     */
    private Integer resolverUsuarioId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .map(Usuario::getId)
                .orElse(null);
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
