package com.siglocc.service;

import com.siglocc.dto.*;
import com.siglocc.entity.Equipo;
import com.siglocc.entity.EstadoAsignacion;
import com.siglocc.entity.EstadoEntrega;
import com.siglocc.entity.EstadoIglesia;
import com.siglocc.repository.*;
import com.siglocc.security.IdentidadJwtException;
import com.siglocc.security.JwtAuthDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio del Dashboard de Logística: un único punto de agregación para los
 * 7 bloques que la pantalla necesita (embudos, pendientes, inventario,
 * literatura y avance por equipo), en vez de que el front cruce 6 endpoints.
 *
 * <p>Todas las consultas filtran por el alcance jerárquico del usuario
 * autenticado (ver {@link #resolverAlcance()}), replicando el patrón ya
 * usado en {@link DashboardService#getConsolidado}.</p>
 */
@Service
public class DashboardLogisticaService {

    private final IglesiaRepository iglesiaRepo;
    private final CapacitacionIglesiaRepository capacitacionRepo;
    private final AsignacionCabeceraRepository asignacionCabeceraRepo;
    private final AsignacionDetalleRepository asignacionDetalleRepo;
    private final DetalleRecepcionContenedorRepository detalleRecepcionRepo;
    private final EntregaIglesiaRepository entregaRepo;
    private final DetalleEntregaIglesiaRepository detalleEntregaRepo;
    private final CategoriaCajaRepository categoriaCajaRepo;
    private final TipoItemRepository tipoItemRepo;
    private final EquipoRepository equipoRepo;

    private static final Map<String, Integer> ORDEN_TIPO = Map.of("ENL", 0, "ERLE", 1, "ERL", 2);

    public DashboardLogisticaService(IglesiaRepository iglesiaRepo,
                                     CapacitacionIglesiaRepository capacitacionRepo,
                                     AsignacionCabeceraRepository asignacionCabeceraRepo,
                                     AsignacionDetalleRepository asignacionDetalleRepo,
                                     DetalleRecepcionContenedorRepository detalleRecepcionRepo,
                                     EntregaIglesiaRepository entregaRepo,
                                     DetalleEntregaIglesiaRepository detalleEntregaRepo,
                                     CategoriaCajaRepository categoriaCajaRepo,
                                     TipoItemRepository tipoItemRepo,
                                     EquipoRepository equipoRepo) {
        this.iglesiaRepo = iglesiaRepo;
        this.capacitacionRepo = capacitacionRepo;
        this.asignacionCabeceraRepo = asignacionCabeceraRepo;
        this.asignacionDetalleRepo = asignacionDetalleRepo;
        this.detalleRecepcionRepo = detalleRecepcionRepo;
        this.entregaRepo = entregaRepo;
        this.detalleEntregaRepo = detalleEntregaRepo;
        this.categoriaCajaRepo = categoriaCajaRepo;
        this.tipoItemRepo = tipoItemRepo;
        this.equipoRepo = equipoRepo;
    }

    /**
     * Devuelve todos los agregados del dashboard logístico para una temporada,
     * filtrados por el alcance visible del usuario autenticado.
     *
     * @param temporadaId temporada a consultar
     * @return respuesta completa con los 7 bloques
     * @throws IllegalArgumentException si {@code temporadaId} es nulo o el tipo de
     *                                  equipo del token no es reconocido
     * @throws IdentidadJwtException    si el token no contiene identidad jerárquica
     */
    @Transactional(readOnly = true)
    public DashboardLogisticaResponse obtener(Integer temporadaId) {
        if (temporadaId == null) {
            throw new IllegalArgumentException("El parámetro 'temporadaId' es obligatorio.");
        }

        List<Integer> equipos = resolverAlcance();
        if (equipos.isEmpty()) {
            return vacio(temporadaId);
        }

        return new DashboardLogisticaResponse(
                temporadaId,
                calcularMomentoActual(temporadaId, equipos),
                construirEmbudoIglesias(temporadaId, equipos),
                construirEmbudoCajas(temporadaId, equipos),
                construirPendientes(temporadaId, equipos),
                construirInventario(temporadaId, equipos),
                construirLiteratura(temporadaId, equipos),
                construirAvanceEquipos(temporadaId, equipos)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLOQUES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Heurística para el momento operativo en curso: no existe un campo de
     * estado en {@code Temporada}, así que se infiere de la actividad ya
     * registrada. Si hay entregas, ya se llegó al Momento 3; si no pero hay
     * capacitaciones, Momento 2; si no, Momento 1.
     */
    private int calcularMomentoActual(Integer temporadaId, List<Integer> equipos) {
        if (entregaRepo.countByTemporadaIdAndEquipoIdIn(temporadaId, equipos) > 0) {
            return 3;
        }
        if (capacitacionRepo.countDistinctIglesiaByTemporadaAndEquipos(temporadaId, equipos) > 0) {
            return 2;
        }
        return 1;
    }

    private EmbudoIglesiasDto construirEmbudoIglesias(Integer temporadaId, List<Integer> equipos) {
        long inscritas = iglesiaRepo.countByTemporadaIdAndEquipoIdIn(temporadaId, equipos);
        long aprobadas = iglesiaRepo.countByTemporadaIdAndEstadoAndEquipoIdIn(
                temporadaId, EstadoIglesia.APROBADA, equipos);
        long capacitadas = capacitacionRepo.countDistinctIglesiaByTemporadaAndEquipos(temporadaId, equipos);
        long conAsignacion = asignacionDetalleRepo.countIglesiasConAsignacionConfirmada(temporadaId, equipos);
        long entregadas = entregaRepo.countDistinctIglesiaIdByTemporadaIdAndEstadoInAndEquipoIdIn(
                temporadaId, List.of(EstadoEntrega.COMPLETADA, EstadoEntrega.PARCIAL), equipos);

        return new EmbudoIglesiasDto(inscritas, aprobadas, capacitadas, conAsignacion, entregadas);
    }

    private EmbudoCajasDto construirEmbudoCajas(Integer temporadaId, List<Integer> equipos) {
        long solicitadas = iglesiaRepo.sumCajasSolicitadasAprobadas(temporadaId, equipos);
        long recibidas = detalleRecepcionRepo.sumCajasRecibidas(temporadaId, equipos);
        long asignadas = asignacionDetalleRepo.sumCajasAsignadasConfirmadas(temporadaId, equipos);
        long entregadas = detalleEntregaRepo.sumCajasEntregadas(temporadaId, equipos);

        return new EmbudoCajasDto(solicitadas, recibidas, asignadas, entregadas);
    }

    private PendientesLogisticaDto construirPendientes(Integer temporadaId, List<Integer> equipos) {
        long aprobadasSinCapacitar = iglesiaRepo.countAprobadasSinCapacitar(temporadaId, equipos);

        long recibidas = detalleRecepcionRepo.sumCajasRecibidas(temporadaId, equipos);
        long asignadas = asignacionDetalleRepo.sumCajasAsignadasConfirmadas(temporadaId, equipos);
        long recibidasSinAsignar = Math.max(0, recibidas - asignadas);

        long enBorrador = asignacionCabeceraRepo.countByTemporadaIdAndEstadoAndEquipoIdIn(
                temporadaId, EstadoAsignacion.BORRADOR, equipos);
        long sinFirma = entregaRepo.countSinFirma(temporadaId, equipos);
        long sinFotosNinos = entregaRepo.countSinFotosNinos(temporadaId, equipos);
        long pendientesRevision = iglesiaRepo.countByTemporadaIdAndEstadoAndEquipoIdIn(
                temporadaId, EstadoIglesia.PENDIENTE, equipos);

        return new PendientesLogisticaDto(
                aprobadasSinCapacitar, recibidasSinAsignar, enBorrador,
                sinFirma, sinFotosNinos, pendientesRevision);
    }

    private List<InventarioCategoriaDto> construirInventario(Integer temporadaId, List<Integer> equipos) {
        Map<Integer, Long> recibidas = toMap(detalleRecepcionRepo.sumCajasRecibidasPorCategoria(temporadaId, equipos));
        Map<Integer, Long> asignadas = toMap(asignacionDetalleRepo.sumCajasAsignadasPorCategoria(temporadaId, equipos));
        Map<Integer, Long> entregadas = toMap(detalleEntregaRepo.sumCajasEntregadasPorCategoria(temporadaId, equipos));

        // Se itera el catálogo completo, no los resultados, para garantizar las 6 filas siempre.
        return categoriaCajaRepo.findAllByOrderById().stream()
                .map(c -> new InventarioCategoriaDto(
                        c.getId(), c.getCodigo(), c.getDescripcion(),
                        recibidas.getOrDefault(c.getId(), 0L),
                        asignadas.getOrDefault(c.getId(), 0L),
                        entregadas.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    private List<LiteraturaDto> construirLiteratura(Integer temporadaId, List<Integer> equipos) {
        Map<Integer, Long> recibida = toMap(detalleRecepcionRepo.sumLiteraturaRecibidaPorTipo(temporadaId, equipos));
        Map<Integer, Long> entregada = toMap(detalleEntregaRepo.sumLiteraturaEntregadaPorTipo(temporadaId, equipos));

        // Catálogo completo ordenado por momento, no los resultados, para garantizar los 6 tipos siempre.
        return tipoItemRepo.findAllByOrderByMomentoAscIdAsc().stream()
                .map(t -> new LiteraturaDto(
                        t.getId(), t.getCodigo(),
                        recibida.getOrDefault(t.getId(), 0L),
                        entregada.getOrDefault(t.getId(), 0L)))
                .toList();
    }

    private List<AvanceEquipoDto> construirAvanceEquipos(Integer temporadaId, List<Integer> equipos) {
        List<Equipo> lista = new ArrayList<>(equipoRepo.findAllById(equipos));
        lista.sort(java.util.Comparator
                .comparing((Equipo e) -> ORDEN_TIPO.getOrDefault(e.getTipo().name(), Integer.MAX_VALUE))
                .thenComparing(Equipo::getNombre));

        return lista.stream()
                .map(e -> {
                    long iglesias = iglesiaRepo.countByEquipoIdAndTemporadaIdAndEstado(
                            e.getId(), temporadaId, EstadoIglesia.APROBADA);
                    long capacitadas = capacitacionRepo.countDistinctIglesiaByTemporadaAndEquipo(
                            temporadaId, e.getId());
                    long cajasAsignadas = asignacionDetalleRepo.sumCajasAsignadasConfirmadasPorEquipo(
                            temporadaId, e.getId());
                    long cajasEntregadas = detalleEntregaRepo.sumCajasEntregadasPorEquipo(
                            temporadaId, e.getId());

                    return new AvanceEquipoDto(
                            e.getId(), e.getNombre(), e.getTipo().name(),
                            iglesias, capacitadas, cajasAsignadas, cajasEntregadas);
                })
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resuelve los equipos cuya información logística puede ver el usuario autenticado.
     * ENL ve el país completo, ERLE ve su clúster (él + sus ERL), ERL ve solo el propio.
     *
     * @return lista de IDs visibles; nunca nula
     * @throws IdentidadJwtException    si el token no contiene identidad jerárquica
     * @throws IllegalArgumentException si el tipo de equipo no es ENL, ERLE ni ERL
     */
    private List<Integer> resolverAlcance() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth.getDetails() instanceof JwtAuthDetails details)) {
            throw new IdentidadJwtException(
                    "El token no contiene identidad jerárquica válida. Vuelve a iniciar sesión.");
        }

        return switch (details.equipoTipo()) {
            case "ENL" -> equipoRepo.findAllIds();
            case "ERLE" -> {
                List<Integer> lista = new ArrayList<>(equipoRepo.findIdsByErleId(details.equipoId()));
                lista.add(details.equipoId());
                yield lista;
            }
            case "ERL" -> List.of(details.equipoId());
            default -> throw new IllegalArgumentException(
                    "Tipo de equipo no reconocido en el token: " + details.equipoTipo());
        };
    }

    /** Convierte el resultado {@code [id, total]} de una query GROUP BY en un mapa. */
    private Map<Integer, Long> toMap(List<Object[]> filas) {
        Map<Integer, Long> mapa = new HashMap<>();
        for (Object[] fila : filas) {
            mapa.put((Integer) fila[0], (Long) fila[1]);
        }
        return mapa;
    }

    /** Respuesta con todos los contadores en 0 pero los catálogos completos, para el caso de alcance vacío. */
    private DashboardLogisticaResponse vacio(Integer temporadaId) {
        List<InventarioCategoriaDto> inventario = categoriaCajaRepo.findAllByOrderById().stream()
                .map(c -> new InventarioCategoriaDto(c.getId(), c.getCodigo(), c.getDescripcion(), 0L, 0L, 0L))
                .toList();
        List<LiteraturaDto> literatura = tipoItemRepo.findAllByOrderByMomentoAscIdAsc().stream()
                .map(t -> new LiteraturaDto(t.getId(), t.getCodigo(), 0L, 0L))
                .toList();

        return new DashboardLogisticaResponse(
                temporadaId, 1,
                new EmbudoIglesiasDto(0, 0, 0, 0, 0),
                new EmbudoCajasDto(0, 0, 0, 0),
                new PendientesLogisticaDto(0, 0, 0, 0, 0, 0),
                inventario, literatura, List.of());
    }
}
