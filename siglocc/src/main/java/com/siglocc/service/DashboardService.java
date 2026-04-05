package com.siglocc.service;

import com.siglocc.dto.DashboardItemResponse;
import com.siglocc.entity.VistaDashboardFinanciero;
import com.siglocc.repository.VistaDashboardFinancieroRepository;
import com.siglocc.security.JwtAuthDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

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
     * @throws IllegalStateException si el token no contiene la identidad jerárquica
     *                               (no debería ocurrir en producción si el login es correcto)
     * @throws IllegalArgumentException si el tipo de equipo no es ENL, ERLE ni ERL
     */
    public List<DashboardItemResponse> getConsolidado(Integer temporadaId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getDetails() instanceof JwtAuthDetails details)) {
            throw new IllegalStateException(
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

        return filas.stream().map(this::toResponse).toList();
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
