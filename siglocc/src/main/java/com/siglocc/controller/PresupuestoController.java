package com.siglocc.controller;

import com.siglocc.dto.CargaMasivaResponse;
import com.siglocc.dto.PresupuestoRequest;
import com.siglocc.dto.PresupuestoResponse;
import com.siglocc.service.PresupuestoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Controlador REST para la ingesta de datos operativos de presupuesto por equipo.
 *
 * <p>Accesible para cualquier usuario autenticado. El servicio valida internamente
 * que los prerequisitos estén cumplidos (parámetros ENL y meta del equipo).</p>
 *
 * <p>Expone dos endpoints:</p>
 * <ul>
 *   <li>{@code POST /api/v1/presupuestos} – Carga manual individual de un equipo.</li>
 *   <li>{@code POST /api/v1/presupuestos/upload} – Carga masiva desde archivo CSV.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/presupuestos")
@SecurityRequirement(name = "bearerAuth")
public class PresupuestoController {

    private final PresupuestoService presupuestoService;

    public PresupuestoController(PresupuestoService presupuestoService) {
        this.presupuestoService = presupuestoService;
    }

    /**
     * Registra los datos de presupuesto de un equipo de forma manual.
     *
     * <p>Si ya existe un registro para la misma combinación equipo+temporada,
     * lo sobreescribe. El mensaje de respuesta incluye el nombre del equipo
     * para confirmación visual del usuario.</p>
     *
     * @param request datos operativos del equipo (equipoId, temporadaId y métricas)
     * @return HTTP 201 con mensaje de confirmación e ID del registro
     */
    @PostMapping
    public ResponseEntity<PresupuestoResponse> crear(@RequestBody PresupuestoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(presupuestoService.guardar(request));
    }

    /**
     * Procesa un archivo CSV con datos de presupuesto de múltiples equipos.
     *
     * <p>El proceso <strong>nunca se detiene ante errores individuales</strong>:
     * procesa todas las filas y reporta un resumen con el detalle de cada fila fallida.
     * Útil para cargas masivas al inicio de temporada.</p>
     *
     * <p><strong>Uso desde Swagger UI:</strong> Usar el campo {@code archivo} de tipo
     * {@code file} para adjuntar el CSV. El {@code Content-Type} debe ser
     * {@code multipart/form-data}.</p>
     *
     * <p><strong>Formato del CSV:</strong></p>
     * <pre>
     * equipo_id,temporada_id,promedio_cm,num_pv,entrenadores_pv,personas_pv,maestros_lga,num_cap,entrenadores_cap,mentoreo_equipos,oracion_cop
     * 2,1,49.5,100,2,3,384,3,12,3,50000.00
     * </pre>
     *
     * @param archivo archivo CSV con los datos de múltiples equipos
     * @return HTTP 200 con resumen: procesados, exitosos, fallidos y lista de errores
     */
    @PostMapping("/upload")
    public ResponseEntity<CargaMasivaResponse> cargarCsv(
            @RequestParam MultipartFile archivo) {
        return ResponseEntity.ok(presupuestoService.procesarCsv(archivo));
    }

    /**
     * Maneja errores de validación: prerequisitos no cumplidos, equipo o datos inválidos.
     * Retorna HTTP 400 con el mensaje del error.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    /**
     * Maneja errores de prerequisito: módulo bloqueado por falta de parámetros ENL.
     * Retorna HTTP 400 con el mensaje del error.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
