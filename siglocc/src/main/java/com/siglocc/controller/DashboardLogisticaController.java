package com.siglocc.controller;

import com.siglocc.dto.DashboardLogisticaResponse;
import com.siglocc.service.DashboardLogisticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador del Dashboard de Logística.
 *
 * <p>Expone un único endpoint que devuelve todos los agregados de la pantalla
 * en una sola llamada, filtrados automáticamente por la jerarquía del JWT.</p>
 *
 * <p>Se prefirió un endpoint agregado sobre el cruce en cliente de los 6
 * endpoints de logística: el payload baja de ~800 KB a ~6 KB y el trabajo
 * de agregación lo resuelve MySQL con {@code GROUP BY}. Errores de identidad
 * de sesión o de alcance se resuelven a 400 vía {@link com.siglocc.controller.GlobalExceptionHandler},
 * sin handlers locales — mismo patrón que {@link DashboardController}.</p>
 */
@RestController
@RequestMapping("/api/v1/logistica/dashboard")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Logística – Dashboard", description = "Agregados del flujo físico de cajas")
public class DashboardLogisticaController {

    private final DashboardLogisticaService service;

    public DashboardLogisticaController(DashboardLogisticaService service) {
        this.service = service;
    }

    /**
     * Retorna los agregados del dashboard logístico filtrados por jerarquía.
     *
     * @param temporadaId temporada a consultar (requerido)
     * @return HTTP 200 con los 7 bloques de agregados
     */
    @Operation(summary = "Obtener agregados del dashboard logístico",
               description = "Embudos de conversión, pendientes, inventario por categoría, "
                           + "literatura y avance por equipo. Filtrado por jerarquía del JWT.")
    @GetMapping
    public ResponseEntity<DashboardLogisticaResponse> obtener(
            @RequestParam Integer temporadaId) {
        return ResponseEntity.ok(service.obtener(temporadaId));
    }
}
