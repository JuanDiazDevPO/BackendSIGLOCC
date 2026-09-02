package com.siglocc.service;

import com.siglocc.dto.*;
import com.siglocc.entity.*;
import com.siglocc.repository.*;
import com.siglocc.security.IdentidadJwtException;
import com.siglocc.security.JwtAuthDetails;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio principal del módulo de Reportes Mensuales.
 *
 * <p>Implementa el ciclo de vida completo de un reporte de gastos:</p>
 * <ol>
 *   <li>{@link #crearReporte} – valida techo presupuestal, crea el cabezote y todos los
 *       rubros en una sola transacción.</li>
 *   <li>{@link #subirSoporte} – adjunta el archivo de evidencia y avanza el estado a
 *       {@code PENDIENTE_ERLE}.</li>
 *   <li>{@link #listarReportes} – lista reportes filtrando por la jerarquía del usuario
 *       autenticado.</li>
 *   <li>{@link #cambiarEstado} – permite a ERLE y ENL aprobar o rechazar los reportes,
 *       registrando quién firmó cada paso.</li>
 * </ol>
 *
 * <p><strong>Seguridad JWT:</strong> El {@code equipoId} y {@code equipoTipo} se obtienen
 * siempre de {@link JwtAuthDetails} (no del body del request). El {@code usuarioId}
 * del aprobador se resuelve buscando por email ({@code sub} del JWT) en
 * {@link UsuarioRepository}.</p>
 *
 * <p><strong>Validación de techo presupuestal:</strong> Antes de persistir un reporte,
 * se consulta {@code vista_control_saldos_enl} para verificar que los montos no superen
 * el saldo disponible. La vista ya refleja todos los reportes aprobados anteriores.</p>
 *
 * <p><strong>Impacto financiero:</strong> Solo los reportes en estado
 * {@link EstadoReporte#APROBADO} alimentan el ejecutado en las vistas MySQL.
 * La transición a APROBADO la realiza únicamente el ENL.</p>
 */
@Service
public class ReporteService {

    private final ReporteMensualRepository reporteRepo;
    private final ReporteDetalleRepository detalleRepo;
    private final ReporteCategoriaRepository categoriaRepo;
    private final VistaControlSaldosRepository saldosRepo;
    private final UsuarioRepository usuarioRepo;
    private final EquipoRepository equipoRepo;
    private final StorageService storageService;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Todos los parámetros son beans singleton gestionados por Spring; no es posible ni necesario hacer copias defensivas.")
    public ReporteService(ReporteMensualRepository reporteRepo,
                          ReporteDetalleRepository detalleRepo,
                          ReporteCategoriaRepository categoriaRepo,
                          VistaControlSaldosRepository saldosRepo,
                          UsuarioRepository usuarioRepo,
                          EquipoRepository equipoRepo,
                          StorageService storageService) {
        this.reporteRepo   = reporteRepo;
        this.detalleRepo   = detalleRepo;
        this.categoriaRepo = categoriaRepo;
        this.saldosRepo    = saldosRepo;
        this.usuarioRepo   = usuarioRepo;
        this.equipoRepo    = equipoRepo;
        this.storageService = storageService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREAR REPORTE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Crea un nuevo reporte mensual con todos sus rubros en una sola transacción.
     *
     * <p><strong>Flujo de validación:</strong></p>
     * <ol>
     *   <li>Extrae {@code equipoId} y {@code equipoTipo} del token JWT.</li>
     *   <li>Valida que el mes sea entre 1 y 12.</li>
     *   <li>Verifica que no exista ya un reporte para esa combinación equipo/temporada/mes/año.</li>
     *   <li>Por cada rubro: valida que la categoría exista y que los equipos ERL solo
     *       usen categorías de familia {@code E}. Acumula totales por bucket.</li>
     *   <li><strong>Validación de techo:</strong> consulta {@code vista_control_saldos_enl}
     *       y compara:
     *       <ul>
     *         <li>Total de rubros {@code E-*} vs {@code saldoEntrenamiento}.</li>
     *         <li>Total de rubros {@code M-*} y {@code O-*} vs {@code saldoMentoreo}.</li>
     *       </ul>
     *       Si algún total supera el saldo disponible, se lanza
     *       {@link IllegalArgumentException} y no se persiste nada.</li>
     *   <li>Guarda el cabezote en estado {@link EstadoReporte#BORRADOR}.</li>
     *   <li>Guarda cada detalle dentro de la misma transacción.</li>
     * </ol>
     *
     * @param request datos del reporte y la lista de rubros
     * @return respuesta completa con el reporte creado
     * @throws IllegalArgumentException si el mes es inválido, el reporte ya existe,
     *                                  alguna categoría no existe, un ERL intenta usar
     *                                  una categoría fuera de familia E, o los montos
     *                                  superan el saldo disponible
     */
    @Transactional
    public ReporteResponse crearReporte(ReporteRequest request) {
        JwtAuthDetails details = obtenerDetails();
        Integer equipoId  = details.equipoId();
        String equipoTipo = details.equipoTipo();

        // Validar rango del mes
        if (request.mes() == null || request.mes() < 1 || request.mes() > 12) {
            throw new IllegalArgumentException("El mes debe ser un valor entre 1 y 12.");
        }

        // Guardia contra duplicados antes de que falle la UNIQUE constraint
        if (reporteRepo.existsByEquipoIdAndTemporadaIdAndMesAndAnio(
                equipoId, request.temporadaId(), request.mes(), request.anio())) {
            throw new IllegalArgumentException(
                    "Ya existe un reporte para el equipo " + equipoId +
                    " en " + request.mes() + "/" + request.anio() +
                    " para la temporada " + request.temporadaId() + ".");
        }

        // Validar categorías, restricciones por rol y acumular totales por bucket
        DetallesValidados validados = validarYConstruirDetalles(request.detalles(), equipoTipo);

        // Validación de techo: comparar totales contra saldo disponible en la vista
        validarTechoPresupuestal(equipoId, request.temporadaId(),
                validados.totalEntrenamiento(), validados.totalMentoreoOtros());

        // Persistir el cabezote
        ReporteMensual reporte = new ReporteMensual();
        reporte.setEquipoId(equipoId);
        reporte.setTemporadaId(request.temporadaId());
        reporte.setMes(request.mes());
        reporte.setAnio(request.anio());
        reporte.setEstado(EstadoReporte.BORRADOR);
        reporte.setFechaCreacion(LocalDateTime.now());
        reporteRepo.save(reporte);

        // Persistir cada detalle dentro de la misma transacción
        for (ReporteDetalle detalle : validados.detalles()) {
            detalle.setReporteId(reporte.getId());
            detalleRepo.save(detalle);
        }

        return construirResponse(reporte, validados.detalles(), obtenerNombreEquipo(equipoId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUBIR SOPORTE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Adjunta el archivo de evidencia (PDF/ZIP) a un reporte en estado BORRADOR
     * y lo avanza automáticamente a {@link EstadoReporte#PENDIENTE_ERLE}.
     *
     * <p>El equipo del usuario (del JWT) debe coincidir con el equipo del reporte
     * para evitar que un usuario suba soporte en nombre de otro equipo.</p>
     *
     * @param id      ID del reporte al que se adjunta el soporte
     * @param archivo archivo multipart enviado por el cliente
     * @return respuesta actualizada con el nombre del archivo y el nuevo estado
     * @throws IllegalArgumentException si el reporte no existe
     * @throws IllegalStateException    si el usuario no tiene permisos o el estado no es BORRADOR
     */
    @Transactional
    public ReporteResponse subirSoporte(Integer id, MultipartFile archivo) {
        JwtAuthDetails details = obtenerDetails();
        Integer equipoId = details.equipoId();

        ReporteMensual reporte = reporteRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reporte no encontrado con id: " + id));

        // Control de acceso: solo el equipo dueño puede subir su soporte
        if (!reporte.getEquipoId().equals(equipoId)) {
            throw new IllegalStateException(
                    "No tiene permisos para modificar el reporte " + id +
                    ". Solo el equipo propietario puede adjuntar el soporte.");
        }

        if (reporte.getEstado() != EstadoReporte.BORRADOR) {
            throw new IllegalStateException(
                    "Solo se puede adjuntar soporte a reportes en estado BORRADOR. " +
                    "Estado actual: " + reporte.getEstado().name());
        }

        String nombreArchivo = storageService.almacenarSoporte(
                archivo, equipoId, reporte.getMes(), reporte.getAnio());

        reporte.setUrlSoporte(nombreArchivo);
        reporte.setEstado(EstadoReporte.PENDIENTE_ERLE);
        reporteRepo.save(reporte);

        List<ReporteDetalle> detalles = detalleRepo.findByReporteId(id);
        return construirResponse(reporte, detalles, obtenerNombreEquipo(equipoId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EDITAR REPORTE RECHAZADO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Corrige los rubros de un reporte rechazado y lo devuelve a estado
     * {@link EstadoReporte#BORRADOR} para que el ERL pueda adjuntar un nuevo
     * soporte y reiniciar el flujo de aprobación.
     *
     * <p><strong>Solo opera sobre reportes en estado {@link EstadoReporte#RECHAZADO}.</strong>
     * Intentar editar un reporte en cualquier otro estado lanza
     * {@link IllegalStateException}.</p>
     *
     * <p><strong>Qué se modifica:</strong></p>
     * <ul>
     *   <li>Los detalles anteriores se eliminan y se reemplazan por los nuevos.</li>
     *   <li>{@code urlSoporte} se borra — el soporte anterior ya no refleja los
     *       montos corregidos; el ERL debe subir uno nuevo.</li>
     *   <li>{@code estado} vuelve a {@link EstadoReporte#BORRADOR}.</li>
     *   <li>{@code aprobadorErleId} y {@code aprobadorEnlId} se limpian.</li>
     *   <li>{@code fechaAprobacionFinal} se limpia.</li>
     * </ul>
     *
     * <p><strong>Qué se conserva:</strong></p>
     * <ul>
     *   <li>{@code observaciones} del rechazo — el ERL puede ver el motivo
     *       mientras corrige los datos.</li>
     *   <li>El período ({@code mes}, {@code anio}, {@code temporadaId}) y el
     *       {@code equipoId} no cambian.</li>
     * </ul>
     *
     * @param id      ID del reporte a corregir
     * @param request nueva lista de rubros con montos corregidos
     * @return respuesta actualizada con el reporte en estado BORRADOR
     * @throws IllegalArgumentException si el reporte no existe, alguna categoría
     *                                  no existe, un ERL usa familia no permitida
     *                                  o los montos superan el saldo disponible
     * @throws IllegalStateException    si el usuario no es dueño del reporte o
     *                                  el reporte no está en estado RECHAZADO
     */
    @Transactional
    public ReporteResponse editarReporte(Integer id, EditarReporteRequest request) {
        JwtAuthDetails details = obtenerDetails();
        Integer equipoId  = details.equipoId();
        String equipoTipo = details.equipoTipo();

        ReporteMensual reporte = reporteRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reporte no encontrado con id: " + id));

        // Control de acceso: solo el equipo dueño puede corregir su reporte
        if (!reporte.getEquipoId().equals(equipoId)) {
            throw new IllegalStateException(
                    "No tiene permisos para editar el reporte " + id +
                    ". Solo el equipo propietario puede corregirlo.");
        }

        // Solo se pueden editar reportes rechazados
        if (reporte.getEstado() != EstadoReporte.RECHAZADO) {
            throw new IllegalStateException(
                    "Solo se pueden editar reportes en estado RECHAZADO. " +
                    "Estado actual: " + reporte.getEstado().name());
        }

        // Validar categorías, restricciones por rol y acumular totales por bucket
        DetallesValidados validados = validarYConstruirDetalles(request.detalles(), equipoTipo);

        // Validar techo presupuestal con los montos corregidos
        validarTechoPresupuestal(equipoId, reporte.getTemporadaId(),
                validados.totalEntrenamiento(), validados.totalMentoreoOtros());

        // Reemplazar detalles: eliminar los anteriores y guardar los nuevos
        detalleRepo.deleteByReporteId(id);
        for (ReporteDetalle detalle : validados.detalles()) {
            detalle.setReporteId(id);
            detalleRepo.save(detalle);
        }

        // Resetear el cabezote a BORRADOR
        reporte.setEstado(EstadoReporte.BORRADOR);
        reporte.setUrlSoporte(null);              // El soporte anterior ya no es válido
        reporte.setAprobadorErleId(null);         // Limpiar auditoría del ciclo anterior
        reporte.setAprobadorEnlId(null);
        reporte.setFechaAprobacionFinal(null);
        // Las observaciones se conservan para que el ERL sepa qué corregir
        reporteRepo.save(reporte);

        return construirResponse(reporte, validados.detalles(), obtenerNombreEquipo(equipoId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LISTAR REPORTES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lista los reportes visibles para el usuario autenticado, aplicando filtrado
     * jerárquico automático basado en el {@code equipoTipo} del token JWT.
     *
     * <ul>
     *   <li><strong>ENL:</strong> todos los reportes de la temporada.</li>
     *   <li><strong>ERLE:</strong> su propio equipo más todos los ERL de su clúster.</li>
     *   <li><strong>ERL:</strong> únicamente sus propios reportes.</li>
     * </ul>
     *
     * @param temporadaId ID de la temporada para filtrar
     * @return lista de reportes visibles para el usuario con todos sus detalles
     */
    public List<ReporteResponse> listarReportes(Integer temporadaId) {
        JwtAuthDetails details = obtenerDetails();
        Integer equipoId  = details.equipoId();
        String equipoTipo = details.equipoTipo();

        List<ReporteMensual> reportes = switch (equipoTipo) {
            case "ENL"  -> reporteRepo.findByTemporadaId(temporadaId);
            case "ERLE" -> reporteRepo.findByErleClusterAndTemporada(equipoId, temporadaId);
            case "ERL"  -> reporteRepo.findByEquipoIdAndTemporadaId(equipoId, temporadaId);
            default -> throw new IllegalArgumentException(
                    "Tipo de equipo no reconocido en el token: " + equipoTipo);
        };

        List<Integer> equipoIds = reportes.stream()
                .map(ReporteMensual::getEquipoId)
                .distinct()
                .toList();
        Map<Integer, String> nombresEquipo = equipoRepo.findAllById(equipoIds).stream()
                .collect(Collectors.toMap(Equipo::getId, Equipo::getNombre));

        return reportes.stream()
                .map(r -> construirResponse(r, detalleRepo.findByReporteId(r.getId()),
                        nombresEquipo.get(r.getEquipoId())))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIAR ESTADO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Cambia el estado de un reporte y registra la auditoría del aprobador.
     *
     * <p><strong>Transiciones permitidas:</strong></p>
     * <ul>
     *   <li><strong>ERLE:</strong> {@code PENDIENTE_ERLE → PENDIENTE_ENL} o {@code RECHAZADO}.</li>
     *   <li><strong>ENL:</strong> {@code PENDIENTE_ENL → APROBADO} o {@code RECHAZADO}.</li>
     * </ul>
     *
     * <p><strong>Auditoría:</strong> El ID del usuario que firma cada paso se obtiene
     * buscando por el email del token JWT en {@link UsuarioRepository} y se guarda en
     * {@code aprobadorErleId} o {@code aprobadorEnlId} según corresponda.</p>
     *
     * <p>Al transicionar a {@link EstadoReporte#APROBADO}, la vista
     * {@code vista_control_saldos_enl} comenzará a incluir los montos de este
     * reporte en el ejecutado del equipo.</p>
     *
     * @param id      ID del reporte a modificar
     * @param request nuevo estado y observaciones opcionales
     * @return respuesta actualizada del reporte
     * @throws IllegalArgumentException si el reporte no existe, el estado destino es inválido
     *                                  o faltan observaciones al rechazar
     * @throws IllegalStateException    si el tipo de equipo no tiene permisos para esa transición
     */
    @Transactional
    public ReporteResponse cambiarEstado(Integer id, CambiarEstadoRequest request) {
        JwtAuthDetails details = obtenerDetails();
        String equipoTipo = details.equipoTipo();

        ReporteMensual reporte = reporteRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reporte no encontrado con id: " + id));

        // Parsear el estado destino con mensaje claro si el valor es inválido
        EstadoReporte nuevoEstado;
        try {
            nuevoEstado = EstadoReporte.valueOf(request.nuevoEstado());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Estado no válido: '" + request.nuevoEstado() +
                    "'. Valores permitidos: PENDIENTE_ENL, APROBADO, RECHAZADO.");
        }

        // Validar que la transición sea coherente con el rol del revisor
        validarTransicion(reporte.getEstado(), nuevoEstado, equipoTipo);

        // El rechazo siempre requiere justificación
        if (nuevoEstado == EstadoReporte.RECHAZADO) {
            if (request.observaciones() == null || request.observaciones().isBlank()) {
                throw new IllegalArgumentException(
                        "El campo 'observaciones' es obligatorio al rechazar un reporte.");
            }
            reporte.setObservaciones(request.observaciones());
        }

        // Registrar fecha de aprobación final cuando el ENL aprueba
        if (nuevoEstado == EstadoReporte.APROBADO) {
            reporte.setFechaAprobacionFinal(LocalDateTime.now());
        }

        // Auditoría: capturar el ID del usuario que está firmando este paso
        Integer aprobadorId = resolverUsuarioId();
        if ("ERLE".equals(equipoTipo)) {
            reporte.setAprobadorErleId(aprobadorId);
        } else if ("ENL".equals(equipoTipo)) {
            reporte.setAprobadorEnlId(aprobadorId);
        }

        reporte.setEstado(nuevoEstado);
        reporteRepo.save(reporte);

        List<ReporteDetalle> detalles = detalleRepo.findByReporteId(id);
        return construirResponse(reporte, detalles, obtenerNombreEquipo(reporte.getEquipoId()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Contenedor inmutable con los detalles ya validados y los totales acumulados
     * por bucket presupuestal. Se usa para evitar duplicar la lógica de validación
     * entre {@link #crearReporte} y {@link #editarReporte}.
     */
    private record DetallesValidados(
            List<ReporteDetalle> detalles,
            BigDecimal totalEntrenamiento,
            BigDecimal totalMentoreoOtros) {}

    /**
     * Valida cada rubro del request y construye la lista de entidades {@link ReporteDetalle},
     * acumulando los totales por bucket presupuestal.
     *
     * <p>Reglas aplicadas por cada rubro:</p>
     * <ul>
     *   <li>La categoría debe existir en {@code reporte_categorias}.</li>
     *   <li>Los equipos ERL solo pueden usar categorías de familia {@code E}.</li>
     *   <li>El monto debe ser 0 o mayor (no nulo, no negativo).</li>
     * </ul>
     *
     * @param requests  lista de rubros del request (create o edit)
     * @param equipoTipo tipo de equipo del usuario autenticado (ERL, ERLE, ENL)
     * @return record con la lista de detalles y los totales E / M+O
     * @throws IllegalArgumentException si alguna categoría no existe, el rol no tiene acceso
     *                                  a esa familia, o el monto es negativo/nulo
     */
    private DetallesValidados validarYConstruirDetalles(List<ReporteDetalleRequest> requests,
                                                        String equipoTipo) {
        List<ReporteDetalle> detalles      = new ArrayList<>();
        BigDecimal totalEntrenamiento      = BigDecimal.ZERO;
        BigDecimal totalMentoreoOtros      = BigDecimal.ZERO;

        for (ReporteDetalleRequest dr : requests) {
            ReporteCategoria categoria = categoriaRepo.findById(dr.categoriaCodigo())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Categoría no encontrada: '" + dr.categoriaCodigo() + "'. " +
                            "Verifique que el código exista en la tabla reporte_categorias."));

            // Los equipos ERL solo pueden reportar gastos de entrenamiento (familia E)
            if ("ERL".equals(equipoTipo) && categoria.getFamilia() != FamiliaCategoria.E) {
                throw new IllegalArgumentException(
                        "El equipo ERL solo puede reportar categorías de la familia 'E'. " +
                        "La categoría '" + dr.categoriaCodigo() + "' pertenece a la familia '" +
                        categoria.getFamilia().name() + "'.");
            }

            if (dr.montoGastado() == null || dr.montoGastado().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "El monto gastado para la categoría '" + dr.categoriaCodigo() +
                        "' debe ser mayor o igual a cero.");
            }

            // Acumular por bucket: E → entrenamiento; M y O → mentoreo/otros
            if (categoria.getFamilia() == FamiliaCategoria.E) {
                totalEntrenamiento = totalEntrenamiento.add(dr.montoGastado());
            } else {
                totalMentoreoOtros = totalMentoreoOtros.add(dr.montoGastado());
            }

            ReporteDetalle detalle = new ReporteDetalle();
            detalle.setCategoriaCodigo(dr.categoriaCodigo());
            detalle.setMontoGastado(dr.montoGastado());
            detalles.add(detalle);
        }

        return new DetallesValidados(detalles, totalEntrenamiento, totalMentoreoOtros);
    }

    /**
     * Verifica que los montos reportados no superen el saldo disponible en la vista
     * {@code vista_control_saldos_enl}.
     *
     * <p>La vista mapea así los buckets:</p>
     * <ul>
     *   <li>Rubros {@code E-*} → {@code saldoEntrenamiento}
     *       ({@code presupuesto_entrenamiento - ejecutado_entrenamiento})</li>
     *   <li>Rubros {@code M-*} y {@code O-*} → {@code saldoMentoreo}
     *       ({@code presupuesto_mentoreo - ejecutado_mentoreo})</li>
     * </ul>
     *
     * <p>Si no existe registro en la vista para ese equipo y temporada (sin presupuesto
     * configurado), se lanza excepción para evitar reportar sobre un presupuesto vacío.</p>
     *
     * @param equipoId            ID del equipo cuyo saldo se consulta
     * @param temporadaId         ID de la temporada
     * @param totalEntrenamiento  suma de todos los rubros de familia E en el reporte
     * @param totalMentoreoOtros  suma de todos los rubros de familia M y O en el reporte
     * @throws IllegalArgumentException si no hay presupuesto configurado o se supera el saldo
     */
    private void validarTechoPresupuestal(Integer equipoId, Integer temporadaId,
                                          BigDecimal totalEntrenamiento,
                                          BigDecimal totalMentoreoOtros) {
        VistaControlSaldos saldos = saldosRepo
                .findById(new VistaControlSaldosId(equipoId, temporadaId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró presupuesto configurado para el equipo " + equipoId +
                        " en la temporada " + temporadaId +
                        ". Verifique que existan parámetros ENL y meta de equipo."));

        BigDecimal saldoEntrenamiento = saldos.getSaldoEntrenamiento();
        BigDecimal saldoMentoreo      = saldos.getSaldoMentoreo();

        if (totalEntrenamiento.compareTo(BigDecimal.ZERO) > 0
                && totalEntrenamiento.compareTo(saldoEntrenamiento) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Los gastos de entrenamiento ($%,.2f) superan el saldo disponible ($%,.2f). " +
                    "El reporte no puede crearse con sobregasto.",
                    totalEntrenamiento, saldoEntrenamiento));
        }

        if (totalMentoreoOtros.compareTo(BigDecimal.ZERO) > 0
                && totalMentoreoOtros.compareTo(saldoMentoreo) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Los gastos de mentoría/otros ($%,.2f) superan el saldo disponible ($%,.2f). " +
                    "El reporte no puede crearse con sobregasto.",
                    totalMentoreoOtros, saldoMentoreo));
        }
    }

    /**
     * Valida que la transición de estado sea permitida para el tipo de equipo
     * que realiza la acción.
     *
     * @param actual     estado actual del reporte
     * @param nuevo      estado destino
     * @param equipoTipo tipo de equipo del usuario autenticado
     * @throws IllegalStateException    si el estado actual no corresponde al paso del revisor
     * @throws IllegalArgumentException si el estado destino no está permitido para ese tipo
     */
    private void validarTransicion(EstadoReporte actual, EstadoReporte nuevo, String equipoTipo) {
        switch (equipoTipo) {
            case "ERLE" -> {
                if (actual != EstadoReporte.PENDIENTE_ERLE) {
                    throw new IllegalStateException(
                            "El ERLE solo puede gestionar reportes en estado PENDIENTE_ERLE. " +
                            "Estado actual: " + actual.name());
                }
                if (nuevo != EstadoReporte.PENDIENTE_ENL && nuevo != EstadoReporte.RECHAZADO) {
                    throw new IllegalArgumentException(
                            "El ERLE solo puede mover reportes a PENDIENTE_ENL o RECHAZADO.");
                }
            }
            case "ENL" -> {
                if (actual != EstadoReporte.PENDIENTE_ENL) {
                    throw new IllegalStateException(
                            "El ENL solo puede gestionar reportes en estado PENDIENTE_ENL. " +
                            "Estado actual: " + actual.name());
                }
                if (nuevo != EstadoReporte.APROBADO && nuevo != EstadoReporte.RECHAZADO) {
                    throw new IllegalArgumentException(
                            "El ENL solo puede mover reportes a APROBADO o RECHAZADO.");
                }
            }
            default -> throw new IllegalStateException(
                    "El tipo de equipo '" + equipoTipo + "' no tiene permisos para cambiar " +
                    "el estado de reportes.");
        }
    }

    /**
     * Extrae el {@link JwtAuthDetails} del contexto de seguridad activo.
     *
     * @return detalles de identidad jerárquica del usuario autenticado
     * @throws IllegalStateException si el token no contiene identidad jerárquica válida
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
     * <p>El JWT tiene el email como {@code sub} (subject), que Spring Security
     * expone en {@code authentication.getName()}. Se hace una consulta a
     * {@link UsuarioRepository} para obtener el ID numérico.</p>
     *
     * @return ID del usuario autenticado, o {@code null} si no se encuentra (caso improbable
     *         dado que el token ya fue validado por el filtro de seguridad)
     */
    private Integer resolverUsuarioId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepo.findByEmail(email)
                .map(Usuario::getId)
                .orElse(null);
    }

    /**
     * Construye el DTO de respuesta a partir de las entidades del reporte y sus detalles.
     *
     * <p>Enriquece cada detalle con el nombre largo y la familia de la categoría,
     * y calcula el monto total como la suma de todos los rubros.</p>
     *
     * @param reporte      entidad cabezote del reporte
     * @param detalles     lista de entidades de detalle asociadas
     * @param nombreEquipo nombre descriptivo del equipo dueño del reporte, o {@code null}
     *                     si el equipo ya no existe
     * @return DTO completo listo para serializar en la respuesta HTTP
     */
    private ReporteResponse construirResponse(ReporteMensual reporte, List<ReporteDetalle> detalles,
                                              String nombreEquipo) {
        List<ReporteDetalleResponse> detalleResponses = detalles.stream()
                .map(d -> {
                    ReporteCategoria cat = categoriaRepo.findById(d.getCategoriaCodigo()).orElse(null);
                    String nombre  = cat != null ? cat.getNombreLargo() : d.getCategoriaCodigo();
                    String familia = cat != null ? cat.getFamilia().name() : "";
                    return new ReporteDetalleResponse(
                            d.getCategoriaCodigo(), nombre, familia, d.getMontoGastado());
                })
                .toList();

        BigDecimal montoTotal = detalles.stream()
                .map(ReporteDetalle::getMontoGastado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReporteResponse(
                reporte.getId(),
                reporte.getEquipoId(),
                nombreEquipo,
                reporte.getTemporadaId(),
                reporte.getMes(),
                reporte.getAnio(),
                reporte.getUrlSoporte(),
                reporte.getEstado().name(),
                reporte.getObservaciones(),
                reporte.getFechaCreacion(),
                reporte.getFechaAprobacionFinal(),
                reporte.getAprobadorErleId(),
                reporte.getAprobadorEnlId(),
                detalleResponses,
                montoTotal
        );
    }

    /**
     * Busca el nombre descriptivo de un equipo por su ID.
     *
     * @param equipoId ID del equipo
     * @return el nombre del equipo, o {@code null} si no existe
     */
    private String obtenerNombreEquipo(Integer equipoId) {
        return equipoRepo.findById(equipoId).map(Equipo::getNombre).orElse(null);
    }
}
