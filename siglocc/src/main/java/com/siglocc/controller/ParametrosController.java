package com.siglocc.controller;

import com.siglocc.dto.ActualizarTasaCambioRequest;
import com.siglocc.dto.ClonarParametrosRequest;
import com.siglocc.dto.ParametrosRequest;
import com.siglocc.dto.ParametrosResponse;
import com.siglocc.service.ParametrosService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para la configuración de parámetros financieros del módulo ENL.
 *
 * <p>Todos los endpoints de este controlador requieren el rol {@code ENL_RECURSOS},
 * ya que son operaciones administrativas que afectan el cálculo de presupuestos
 * de toda la red de equipos.</p>
 *
 * <p>Expone tres endpoints:</p>
 * <ul>
 *   <li>{@code POST /api/v1/parametros} – Crear o actualizar parámetros de una temporada.</li>
 *   <li>{@code POST /api/v1/parametros/clonar} – Clonar parámetros entre temporadas.</li>
 *   <li>{@code PATCH /api/v1/parametros/{temporadaId}/tasa-cambio} – Actualizar solo la TRM.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/parametros")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ENL_RECURSOS')")
public class ParametrosController {

    private final ParametrosService parametrosService;

    public ParametrosController(ParametrosService parametrosService) {
        this.parametrosService = parametrosService;
    }

    /**
     * Crea o actualiza los parámetros financieros para una temporada.
     *
     * <p>Si ya existen parámetros para el {@code temporadaId} del request, los actualiza.
     * Si no, crea un nuevo registro. Este endpoint desbloquea automáticamente el módulo
     * de presupuestos para la temporada configurada.</p>
     *
     * @param request todos los parámetros financieros (tasa, costos unitarios, etc.)
     * @return HTTP 201 con confirmación y el ID del registro
     */
    @PostMapping
    public ResponseEntity<ParametrosResponse> guardar(@RequestBody ParametrosRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(parametrosService.guardar(request));
    }

    /**
     * Clona los parámetros de una temporada pasada a una nueva temporada.
     *
     * <p>Útil al inicio de cada temporada para no digitar todos los valores desde cero.
     * Generalmente el coordinador ENL solo necesita actualizar la tasa de cambio
     * después de clonar.</p>
     *
     * @param request IDs de la temporada origen y la temporada destino
     * @return HTTP 201 con confirmación del clonado
     */
    @PostMapping("/clonar")
    public ResponseEntity<ParametrosResponse> clonar(@RequestBody ClonarParametrosRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(parametrosService.clonar(request));
    }

    /**
     * Actualiza únicamente la tasa de cambio (TRM) de una temporada.
     *
     * <p><strong>Alto impacto:</strong> Al ejecutarse, todas las vistas de presupuesto
     * de todos los equipos de la temporada recalculan sus valores en COP automáticamente.
     * No se requiere ningún cambio adicional en datos de equipos.</p>
     *
     * @param temporadaId ID de la temporada cuya TRM se actualizará
     * @param request     nuevo valor de la tasa de cambio en COP/USD
     * @return HTTP 200 con confirmación y mensaje del impacto
     */
    @PatchMapping("/{temporadaId}/tasa-cambio")
    public ResponseEntity<ParametrosResponse> actualizarTasaCambio(
            @PathVariable Integer temporadaId,
            @RequestBody ActualizarTasaCambioRequest request) {
        return ResponseEntity.ok(parametrosService.actualizarTasaCambio(temporadaId, request));
    }

    /**
     * Maneja errores de validación de negocio (temporada origen sin parámetros, equipo inexistente).
     * Retorna HTTP 400 con un mensaje descriptivo.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    /**
     * Maneja errores de estado inválido (ej: temporada destino ya tiene parámetros).
     * Retorna HTTP 400 con un mensaje descriptivo.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
