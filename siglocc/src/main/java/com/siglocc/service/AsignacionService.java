package com.siglocc.service;

import com.siglocc.dto.AjusteAsignacionRequest;
import com.siglocc.dto.AsignacionDetalleResponse;
import com.siglocc.dto.AsignacionResponse;
import com.siglocc.entity.AsignacionCabecera;
import com.siglocc.entity.AsignacionDetalle;
import com.siglocc.entity.CategoriaCaja;
import com.siglocc.entity.EstadoAsignacion;
import com.siglocc.entity.EstadoIglesia;
import com.siglocc.entity.Iglesia;
import com.siglocc.entity.TipoItem;
import com.siglocc.repository.AsignacionCabeceraRepository;
import com.siglocc.repository.AsignacionDetalleRepository;
import com.siglocc.repository.CategoriaCajaRepository;
import com.siglocc.repository.DetalleRecepcionContenedorRepository;
import com.siglocc.repository.IglesiaRepository;
import com.siglocc.repository.TipoItemRepository;
import com.siglocc.security.JwtAuthDetails;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Motor de asignación inteligente de cajas OCC y literatura.
 *
 * <p>Implementa el <strong>Método de Hamilton</strong> (mayor residuo) para distribuir
 * el inventario disponible de forma proporcional entre las iglesias aprobadas,
 * garantizando que la suma de asignaciones sea exactamente igual al stock disponible
 * (inventario llega a cero).</p>
 *
 * <h3>Flujo principal</h3>
 * <ol>
 *   <li>{@link #generarAsignacionAutomatica} — un clic genera la corrida en estado BORRADOR.</li>
 *   <li>{@link #ajustarLinea} — el coordinador puede modificar líneas individuales.</li>
 *   <li>{@link #confirmar} — cierra la corrida (CONFIRMADA) y descuenta el inventario.</li>
 * </ol>
 */
@Service
public class AsignacionService {

    private final AsignacionCabeceraRepository cabeceraRepo;
    private final AsignacionDetalleRepository detalleRepo;
    private final IglesiaRepository iglesiaRepo;
    private final CategoriaCajaRepository categoriaCajaRepo;
    private final TipoItemRepository tipoItemRepo;
    private final DetalleRecepcionContenedorRepository detalleRecepcionRepo;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Beans singletons gestionados por Spring.")
    public AsignacionService(AsignacionCabeceraRepository cabeceraRepo,
                             AsignacionDetalleRepository detalleRepo,
                             IglesiaRepository iglesiaRepo,
                             CategoriaCajaRepository categoriaCajaRepo,
                             TipoItemRepository tipoItemRepo,
                             DetalleRecepcionContenedorRepository detalleRecepcionRepo) {
        this.cabeceraRepo        = cabeceraRepo;
        this.detalleRepo         = detalleRepo;
        this.iglesiaRepo         = iglesiaRepo;
        this.categoriaCajaRepo   = categoriaCajaRepo;
        this.tipoItemRepo        = tipoItemRepo;
        this.detalleRecepcionRepo = detalleRecepcionRepo;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GENERACIÓN AUTOMÁTICA (MÉTODO HAMILTON)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Genera una corrida de asignación automática para el equipo y temporada dados.
     *
     * <p>Pasos del algoritmo:</p>
     * <ol>
     *   <li>Carga las iglesias APROBADAS con cajas solicitadas &gt; 0.</li>
     *   <li>Para cada categoría de caja y tipo de literatura, calcula el stock disponible
     *       (recibido − ya confirmado).</li>
     *   <li>Aplica la distribución Hamilton sobre ese stock.</li>
     *   <li>Persiste la cabecera en BORRADOR y todos los detalles.</li>
     * </ol>
     *
     * @param equipoId    equipo que solicita la asignación (extraído del JWT en el controller)
     * @param temporadaId temporada
     * @return la corrida generada en estado BORRADOR lista para revisión
     * @throws IllegalStateException si no hay iglesias aprobadas con cajas solicitadas
     */
    @Transactional
    public AsignacionResponse generarAsignacionAutomatica(Integer equipoId, Integer temporadaId) {

        List<Iglesia> iglesiasAprobadas = iglesiaRepo.findByEquipoIdAndTemporadaIdAndEstado(
                equipoId, temporadaId, EstadoIglesia.APROBADA);

        List<Iglesia> iglesias = iglesiasAprobadas.stream()
                .filter(i -> i.getCajasSolicitadas() != null && i.getCajasSolicitadas() > 0)
                .toList();

        if (iglesias.isEmpty()) {
            throw new IllegalStateException(
                    "No hay iglesias APROBADAS con cajas solicitadas para este equipo y temporada.");
        }

        int totalSolicitado = iglesias.stream()
                .mapToInt(Iglesia::getCajasSolicitadas)
                .sum();

        List<CategoriaCaja> categorias = categoriaCajaRepo.findAll();
        List<TipoItem>      tiposItem  = tipoItemRepo.findAll();

        // ── Calcular disponible por categoría de caja ─────────────────────
        int totalCajasDisponibles = 0;
        Map<Integer, Integer> disponiblePorCategoria = new LinkedHashMap<>();
        for (CategoriaCaja cat : categorias) {
            int recibido   = detalleRecepcionRepo.sumCajasByEquipoAndTemporadaAndCategoria(
                    equipoId, temporadaId, cat.getId());
            int confirmado = detalleRepo.sumAsignadoCajas(equipoId, temporadaId, cat.getId());
            int disponible = Math.max(0, recibido - confirmado);
            disponiblePorCategoria.put(cat.getId(), disponible);
            totalCajasDisponibles += disponible;
        }

        // ── Calcular disponible por tipo de literatura ────────────────────
        Map<Integer, Integer> disponiblePorItem = new LinkedHashMap<>();
        for (TipoItem item : tiposItem) {
            int recibido   = detalleRecepcionRepo.sumLiteraturaByEquipoAndTemporadaAndTipo(
                    equipoId, temporadaId, item.getId());
            int confirmado = detalleRepo.sumAsignadoLiteratura(equipoId, temporadaId, item.getId());
            int disponible = Math.max(0, recibido - confirmado);
            disponiblePorItem.put(item.getId(), disponible);
        }

        // ── Factor de reducción (informativo) ────────────────────────────
        BigDecimal factorReduccion;
        if (totalCajasDisponibles >= totalSolicitado) {
            factorReduccion = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        } else if (totalSolicitado == 0) {
            factorReduccion = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        } else {
            factorReduccion = BigDecimal.valueOf(totalCajasDisponibles)
                    .divide(BigDecimal.valueOf(totalSolicitado), 4, RoundingMode.HALF_DOWN);
        }

        // ── Crear cabecera en BORRADOR ────────────────────────────────────
        AsignacionCabecera cabecera = new AsignacionCabecera();
        cabecera.setEquipoId(equipoId);
        cabecera.setTemporadaId(temporadaId);
        cabecera.setFechaGeneracion(LocalDateTime.now());
        cabecera.setGeneradaAutomaticamente(true);
        cabecera.setEstado(EstadoAsignacion.BORRADOR);
        cabecera.setTotalCajasDisponibles(totalCajasDisponibles);
        cabecera.setTotalCajasSolicitadas(totalSolicitado);
        cabecera.setFactorReduccion(factorReduccion);
        cabeceraRepo.save(cabecera);

        // ── Generar detalles con distribución Hamilton ────────────────────
        List<AsignacionDetalle> detalles = new ArrayList<>();

        for (CategoriaCaja cat : categorias) {
            int disponible = disponiblePorCategoria.get(cat.getId());
            Map<Integer, Integer> distribucion = distribuirHamilton(iglesias, disponible);
            for (Map.Entry<Integer, Integer> entry : distribucion.entrySet()) {
                AsignacionDetalle det = new AsignacionDetalle();
                det.setCabeceraId(cabecera.getId());
                det.setIglesiaId(entry.getKey());
                det.setCategoriaCajaId(cat.getId());
                det.setCantidadAsignada(entry.getValue());
                det.setAjustadaManualmente(false);
                detalles.add(det);
            }
        }

        for (TipoItem item : tiposItem) {
            int disponible = disponiblePorItem.get(item.getId());
            Map<Integer, Integer> distribucion = distribuirHamilton(iglesias, disponible);
            for (Map.Entry<Integer, Integer> entry : distribucion.entrySet()) {
                AsignacionDetalle det = new AsignacionDetalle();
                det.setCabeceraId(cabecera.getId());
                det.setIglesiaId(entry.getKey());
                det.setTipoItemId(item.getId());
                det.setCantidadAsignada(entry.getValue());
                det.setAjustadaManualmente(false);
                detalles.add(det);
            }
        }

        detalleRepo.saveAll(detalles);

        return construirResponse(cabecera, detalles, iglesias, categorias, tiposItem);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AJUSTE MANUAL DE UNA LÍNEA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ajusta manualmente la cantidad asignada a una iglesia para una categoría o ítem.
     *
     * <p>Solo es posible mientras la cabecera esté en estado {@link EstadoAsignacion#BORRADOR}.</p>
     *
     * @param cabeceraId ID de la cabecera de asignación
     * @param request    datos del ajuste: iglesia, categoría/ítem y nueva cantidad
     * @return la asignación completa actualizada
     * @throws IllegalArgumentException si la cabecera o la línea no existen, o la cantidad es inválida
     * @throws IllegalStateException    si la cabecera no está en BORRADOR
     */
    @Transactional
    public AsignacionResponse ajustarLinea(Integer cabeceraId, AjusteAsignacionRequest request) {
        AsignacionCabecera cabecera = cabeceraRepo.findById(cabeceraId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Asignación no encontrada con id: " + cabeceraId));

        if (cabecera.getEstado() != EstadoAsignacion.BORRADOR) {
            throw new IllegalStateException(
                    "Solo se pueden ajustar asignaciones en estado BORRADOR.");
        }
        if (request.nuevaCantidad() == null || request.nuevaCantidad() < 0) {
            throw new IllegalArgumentException("La nueva cantidad debe ser 0 o mayor.");
        }

        boolean tieneCaja = request.categoriaCajaId() != null;
        boolean tieneItem = request.tipoItemId() != null;
        if (tieneCaja == tieneItem) {
            throw new IllegalArgumentException(
                    "Se debe indicar exactamente uno de categoriaCajaId o tipoItemId.");
        }

        AsignacionDetalle detalle;
        if (tieneCaja) {
            detalle = detalleRepo.findByCabeceraIdAndIglesiaIdAndCategoriaCajaId(
                    cabeceraId, request.iglesiaId(), request.categoriaCajaId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Línea de asignación no encontrada para la iglesia y categoría indicadas."));
        } else {
            detalle = detalleRepo.findByCabeceraIdAndIglesiaIdAndTipoItemId(
                    cabeceraId, request.iglesiaId(), request.tipoItemId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Línea de asignación no encontrada para la iglesia e ítem indicados."));
        }

        detalle.setCantidadAsignada(request.nuevaCantidad());
        detalle.setAjustadaManualmente(true);
        detalleRepo.save(detalle);

        return cargarRespuestaCompleta(cabecera);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIRMACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Confirma la asignación, cambiando su estado de BORRADOR a CONFIRMADA.
     *
     * <p>A partir de este momento el inventario queda descontado y no se pueden
     * hacer más ajustes manuales.</p>
     *
     * @param cabeceraId ID de la cabecera a confirmar
     * @return la asignación en estado CONFIRMADA
     * @throws IllegalArgumentException si la cabecera no existe
     * @throws IllegalStateException    si ya fue confirmada anteriormente
     */
    @Transactional
    public AsignacionResponse confirmar(Integer cabeceraId) {
        AsignacionCabecera cabecera = cabeceraRepo.findById(cabeceraId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Asignación no encontrada con id: " + cabeceraId));

        if (cabecera.getEstado() != EstadoAsignacion.BORRADOR) {
            throw new IllegalStateException(
                    "La asignación ya fue confirmada anteriormente.");
        }

        cabecera.setEstado(EstadoAsignacion.CONFIRMADA);
        cabeceraRepo.save(cabecera);

        return cargarRespuestaCompleta(cabecera);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LISTADO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lista las corridas de asignación visibles para el usuario autenticado.
     *
     * @param temporadaId temporada a consultar
     * @return lista de corridas con sus detalles, orden descendente por fecha
     */
    public List<AsignacionResponse> listar(Integer temporadaId) {
        JwtAuthDetails details = obtenerDetails();
        Integer equipoId  = details.equipoId();
        String  equipoTipo = details.equipoTipo();

        List<AsignacionCabecera> cabeceras = switch (equipoTipo) {
            case "ENL"  -> cabeceraRepo.findByTemporadaIdOrderByFechaGeneracionDesc(temporadaId);
            case "ERLE" -> cabeceraRepo.findByErleClusterAndTemporadaOrderByFechaGeneracionDesc(
                    equipoId, temporadaId);
            case "ERL"  -> cabeceraRepo.findByEquipoIdAndTemporadaIdOrderByFechaGeneracionDesc(
                    equipoId, temporadaId);
            default -> throw new IllegalArgumentException(
                    "Tipo de equipo no reconocido: " + equipoTipo);
        };

        List<CategoriaCaja> categorias = categoriaCajaRepo.findAll();
        List<TipoItem>      tiposItem  = tipoItemRepo.findAll();

        return cabeceras.stream()
                .map(cab -> {
                    List<AsignacionDetalle> dets = detalleRepo.findByCabeceraId(cab.getId());
                    List<Iglesia> iglesias = iglesiaRepo.findByEquipoIdAndTemporadaId(
                            cab.getEquipoId(), cab.getTemporadaId());
                    return construirResponse(cab, dets, iglesias, categorias, tiposItem);
                })
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODO HAMILTON (MAYOR RESIDUO)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Distribuye {@code total} unidades entre las iglesias de forma proporcional
     * a sus cajas solicitadas, garantizando que la suma de asignaciones sea
     * exactamente {@code total} (inventario llega a cero).
     *
     * <p>Algoritmo:</p>
     * <ol>
     *   <li>Calcula la cuota exacta de cada iglesia: {@code (solicitado / totalSolicitado) × total}.</li>
     *   <li>Asigna la parte entera (floor) a cada iglesia.</li>
     *   <li>Distribuye las unidades sobrantes una a una a las iglesias con mayor parte fraccionaria.</li>
     * </ol>
     *
     * @param iglesias lista de iglesias con cajas solicitadas &gt; 0
     * @param total    unidades totales a distribuir
     * @return mapa iglesia_id → cantidad asignada; la suma es exactamente {@code total}
     */
    private Map<Integer, Integer> distribuirHamilton(List<Iglesia> iglesias, int total) {
        if (total == 0) {
            return iglesias.stream()
                    .collect(Collectors.toMap(Iglesia::getId, i -> 0));
        }

        int totalSolicitado = iglesias.stream()
                .mapToInt(i -> i.getCajasSolicitadas() != null ? i.getCajasSolicitadas() : 0)
                .sum();

        if (totalSolicitado == 0) {
            // Equal distribution when no church has explicitly requested boxes
            int base      = total / iglesias.size();
            int sobrante  = total % iglesias.size();
            Map<Integer, Integer> resultado = new LinkedHashMap<>();
            List<Integer> ids = iglesias.stream().map(Iglesia::getId).toList();
            for (int i = 0; i < ids.size(); i++) {
                resultado.put(ids.get(i), base + (i < sobrante ? 1 : 0));
            }
            return resultado;
        }

        // Step 1 – exact quotas and integer bases
        Map<Integer, Double> exactos = new LinkedHashMap<>();
        Map<Integer, Integer> bases  = new LinkedHashMap<>();
        for (Iglesia ig : iglesias) {
            int    sol    = ig.getCajasSolicitadas() != null ? ig.getCajasSolicitadas() : 0;
            double exacto = (double) sol / totalSolicitado * total;
            exactos.put(ig.getId(), exacto);
            bases.put(ig.getId(), (int) exacto);   // floor
        }

        // Step 2 – how many units still need to be assigned
        int asignadoBase = bases.values().stream().mapToInt(Integer::intValue).sum();
        int sobrante     = total - asignadoBase;

        // Step 3 – give 1 extra unit to the churches with the largest fractional parts
        List<Integer> ordenados = iglesias.stream()
                .sorted(Comparator.comparingDouble(ig -> {
                    double exacto = exactos.get(ig.getId());
                    return -(exacto - Math.floor(exacto));   // descending
                }))
                .map(Iglesia::getId)
                .toList();

        Map<Integer, Integer> resultado = new HashMap<>(bases);
        for (int i = 0; i < sobrante && i < ordenados.size(); i++) {
            resultado.merge(ordenados.get(i), 1, Integer::sum);
        }
        return resultado;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS DE RESPUESTA
    // ─────────────────────────────────────────────────────────────────────────

    /** Carga todo lo necesario para construir la respuesta de una cabecera existente. */
    private AsignacionResponse cargarRespuestaCompleta(AsignacionCabecera cabecera) {
        List<AsignacionDetalle> detalles = detalleRepo.findByCabeceraId(cabecera.getId());
        List<Iglesia>           iglesias = iglesiaRepo.findByEquipoIdAndTemporadaId(
                cabecera.getEquipoId(), cabecera.getTemporadaId());
        List<CategoriaCaja>     categorias = categoriaCajaRepo.findAll();
        List<TipoItem>          tiposItem  = tipoItemRepo.findAll();
        return construirResponse(cabecera, detalles, iglesias, categorias, tiposItem);
    }

    /** Construye el DTO de respuesta con lookups en memoria para evitar N+1. */
    private AsignacionResponse construirResponse(AsignacionCabecera cabecera,
                                                 List<AsignacionDetalle> detalles,
                                                 List<Iglesia> iglesias,
                                                 List<CategoriaCaja> categorias,
                                                 List<TipoItem> tiposItem) {
        Map<Integer, String>  nombreIglesia = iglesias.stream()
                .collect(Collectors.toMap(Iglesia::getId, Iglesia::getNombre));
        Map<Integer, CategoriaCaja> catMap  = categorias.stream()
                .collect(Collectors.toMap(CategoriaCaja::getId, c -> c));
        Map<Integer, TipoItem>      itemMap = tiposItem.stream()
                .collect(Collectors.toMap(TipoItem::getId, t -> t));

        List<AsignacionDetalleResponse> detalleResponses = detalles.stream()
                .map(d -> {
                    String nombre    = nombreIglesia.getOrDefault(d.getIglesiaId(), "");
                    String codCat    = null;
                    String descCat   = null;
                    String codItem   = null;

                    if (d.getCategoriaCajaId() != null) {
                        CategoriaCaja cat = catMap.get(d.getCategoriaCajaId());
                        if (cat != null) {
                            codCat  = cat.getCodigo();
                            descCat = cat.getDescripcion();
                        }
                    }
                    if (d.getTipoItemId() != null) {
                        TipoItem item = itemMap.get(d.getTipoItemId());
                        if (item != null) {
                            codItem = item.getCodigo();
                        }
                    }
                    return new AsignacionDetalleResponse(
                            d.getIglesiaId(), nombre,
                            d.getCategoriaCajaId(), codCat, descCat,
                            d.getTipoItemId(), codItem,
                            d.getCantidadAsignada(), d.getAjustadaManualmente());
                })
                .toList();

        return new AsignacionResponse(
                cabecera.getId(),
                cabecera.getEquipoId(),
                cabecera.getTemporadaId(),
                cabecera.getFechaGeneracion(),
                cabecera.getGeneradaAutomaticamente(),
                cabecera.getEstado().name(),
                cabecera.getTotalCajasDisponibles(),
                cabecera.getTotalCajasSolicitadas(),
                cabecera.getFactorReduccion(),
                cabecera.getObservaciones(),
                detalleResponses);
    }

    private JwtAuthDetails obtenerDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth.getDetails() instanceof JwtAuthDetails details)) {
            throw new IllegalStateException(
                    "El token no contiene identidad jerárquica válida.");
        }
        return details;
    }
}
