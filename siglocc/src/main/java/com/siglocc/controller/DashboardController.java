package com.siglocc.controller;

import com.siglocc.dto.DashboardItemResponse;
import com.siglocc.service.DashboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para el Dashboard financiero consolidado.
 *
 * <p>Expone un único endpoint que devuelve datos filtrados automáticamente
 * según el nivel jerárquico del usuario autenticado. El Front-end siempre
 * llama al mismo endpoint; el Backend decide qué datos mostrar.</p>
 *
 * <p><strong>Niveles de visibilidad:</strong></p>
 * <ul>
 *   <li><strong>ENL</strong>: Ve todos los equipos del país (visión 360°).</li>
 *   <li><strong>ERLE</strong>: Ve su clúster regional (él mismo + sus ERL).</li>
 *   <li><strong>ERL</strong>: Ve únicamente sus propios números.</li>
 * </ul>
 *
 * <p>El filtrado se basa en los claims {@code equipoId} y {@code equipoTipo}
 * del JWT. Nunca en parámetros enviados por el cliente.</p>
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Retorna el consolidado financiero filtrado por jerarquía de mando.
     *
     * <p>El parámetro {@code temporadaId} es el único dato que el cliente envía.
     * La identidad del usuario (y qué datos puede ver) se toma del JWT.</p>
     *
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>GET /api/v1/dashboard/consolidado?temporadaId=1</pre>
     *
     * @param temporadaId ID de la temporada a consultar (requerido)
     * @return HTTP 200 con la lista de filas del dashboard visibles para el usuario
     */
    @GetMapping("/consolidado")
    public ResponseEntity<List<DashboardItemResponse>> getConsolidado(
            @RequestParam Integer temporadaId) {
        return ResponseEntity.ok(dashboardService.getConsolidado(temporadaId));
    }

    /**
     * Maneja errores de token inválido o tipo de equipo no reconocido.
     * Retorna HTTP 400 con el mensaje descriptivo del error.
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleErrors(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
