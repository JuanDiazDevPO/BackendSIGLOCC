package com.siglocc.service;

import com.siglocc.dto.DashboardItemResponse;
import com.siglocc.entity.VistaDashboardFinanciero;
import com.siglocc.repository.VistaDashboardFinancieroRepository;
import com.siglocc.security.IdentidadJwtException;
import com.siglocc.security.JwtAuthDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Servicio que implementa el algoritmo de filtrado jerárquico del Dashboard financiero.
 *
 * <p>Es el núcleo del sistema de visibilidad por mando. Extrae la identidad
 * jerárquica del usuario del {@code SecurityContextHolder} (inyectada por
 * {@link com.siglocc.security.JwtAuthFilter} al validar el JWT) y ejecuta
 * la consulta correspondiente según el nivel del usuario:</p>
 *
 * <table border="1">
 *   <tr><th>Nivel</th><th>Lo que puede ver</th><th>Query ejecutado</th></tr>
 *   <tr><td>ENL</td><td>Todos los equipos del país</td><td>findAllByTemporada</td></tr>
 *   <tr><td>ERLE</td><td>Su clúster regional (sí mismo + sus ERL)</td><td>findByErleAndTemporada</td></tr>
 *   <tr><td>ERL</td><td>Solo su propio registro</td><td>findByEquipoAndTemporada</td></tr>
 * </table>
 *
 * <p><strong>Principio de seguridad:</strong> El servicio nunca confía en un
 * {@code equipoId} enviado por el cliente en la URL. Siempre usa el {@code equipoId}
 * extraído del token JWT firmado por el servidor.</p>
 */
@Service
public class DashboardService {

    /** Orden de lectura de cada tipo de equipo dentro de un mismo clúster ERLE: ENL, luego ERLE, luego sus ERL. */
    private static final Map<String, Integer> ORDEN_TIPO = Map.of("ENL", 0, "ERLE", 1, "ERL", 2);

    /**
     * Ordena el consolidado para que el front pueda indentar la jerarquía sin
     * necesitar {@code erleId}/{@code enlId} en el DTO: agrupa cada ERLE con sus
     * ERL subordinados usando {@code coalesce(erleId, equipoId)} como clave de
     * clúster, y dentro de un mismo clúster respeta ENL → ERLE → ERL.
     *
     * <p><strong>{@code nullsFirst} en {@code enlId}, no {@code nullsLast}:</strong>
     * el equipo ENL es la raíz de la jerarquía y su {@code enl_id} siempre es
     * {@code NULL} (no tiene padre) — con {@code nullsLast} el ENL terminaba
     * al final de la lista en vez de primero.</p>
     */
    private static final Comparator<VistaDashboardFinanciero> ORDEN_JERARQUICO = Comparator
            .comparing((VistaDashboardFinanciero v) -> v.getEnlId(),
                    Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(v -> v.getErleId() != null ? v.getErleId() : v.getId().getEquipoId())
            .thenComparing(v -> ORDEN_TIPO.getOrDefault(v.getEquipoTipo(), Integer.MAX_VALUE))
            .thenComparing(VistaDashboardFinanciero::getEquipoNombre,
                    Comparator.nullsLast(Comparator.naturalOrder()));

    private final VistaDashboardFinancieroRepository dashboardRepo;

    public DashboardService(VistaDashboardFinancieroRepository dashboardRepo) {
        this.dashboardRepo = dashboardRepo;
    }

    /**
     * Retorna el consolidado financiero filtrado según el nivel jerárquico del usuario.
     *
     * <p><strong>Flujo interno:</strong></p>
     * <ol>
     *   <li>Extrae el {@link JwtAuthDetails} del {@code SecurityContextHolder}
     *       para obtener {@code equipoId} y {@code equipoTipo} del usuario autenticado.</li>
     *   <li>Aplica el switch por {@code equipoTipo} para ejecutar la query correcta.</li>
     *   <li>Mapea cada fila de la vista a un {@link DashboardItemResponse}.</li>
     * </ol>
     *
     * @param temporadaId ID de la temporada a consultar (viene como query param del request)
     * @return lista de filas del dashboard visibles para el usuario según su jerarquía
     * @throws IdentidadJwtException si el token no contiene la identidad jerárquica
     *                               (no debería ocurrir en producción si el login es correcto)
     * @throws IllegalArgumentException si el tipo de equipo no es ENL, ERLE ni ERL
     */
    public List<DashboardItemResponse> getConsolidado(Integer temporadaId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getDetails() instanceof JwtAuthDetails details)) {
            throw new IdentidadJwtException(
                    "El token no contiene identidad jerárquica. Vuelve a iniciar sesión.");
        }

        Integer equipoId  = details.equipoId();
        String equipoTipo = details.equipoTipo();

        List<VistaDashboardFinanciero> filas = switch (equipoTipo) {
            // ENL: visión 360° — ve todo el país
            case "ENL"  -> dashboardRepo.findAllByTemporada(temporadaId);

            // ERLE: visión de clúster — su fila + las de sus ERL subordinados
            case "ERLE" -> dashboardRepo.findByErleAndTemporada(equipoId, temporadaId);

            // ERL: visión de célula — solo su propia fila
            case "ERL"  -> dashboardRepo.findByEquipoAndTemporada(equipoId, temporadaId);

            default -> throw new IllegalArgumentException(
                    "Tipo de equipo no reconocido en el token: " + equipoTipo);
        };

        return filas.stream().sorted(ORDEN_JERARQUICO).map(this::toResponse).toList();
    }

    /**
     * Convierte una fila de la vista en el DTO de respuesta del Dashboard.
     *
     * @param v fila de la vista {@code vista_dashboard_financiero}
     * @return DTO listo para serializar a JSON
     */
    private DashboardItemResponse toResponse(VistaDashboardFinanciero v) {
        return new DashboardItemResponse(
                v.getId().getEquipoId(),
                v.getEquipoNombre(),
                v.getEquipoTipo(),
                v.getPresupuestoEntrenamiento(),
                v.getEjecutadoEntrenamiento(),
                v.getSaldoEntrenamiento(),
                v.getPresupuestoMentoreo(),
                v.getEjecutadoMentoreo(),
                v.getSaldoMentoreo(),
                v.getGranTotalPresupuesto(),
                v.getGranTotalEjecutado(),
                v.getGranTotalSaldo()
        );
    }
}
