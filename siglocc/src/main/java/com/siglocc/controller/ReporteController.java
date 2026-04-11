package com.siglocc.controller;

import com.siglocc.dto.CambiarEstadoRequest;
import com.siglocc.dto.ReporteRequest;
import com.siglocc.dto.ReporteResponse;
import com.siglocc.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST del módulo de Reportes Mensuales.
 *
 * <p>Expone cuatro endpoints que cubren el ciclo de vida completo de un reporte:</p>
 * <ol>
 *   <li>{@code POST /api/v1/reportes} – crear reporte con rubros.</li>
 *   <li>{@code PUT  /api/v1/reportes/{id}/soporte} – adjuntar archivo de evidencia.</li>
 *   <li>{@code GET  /api/v1/reportes?temporadaId=} – listar reportes según jerarquía.</li>
 *   <li>{@code PATCH /api/v1/reportes/{id}/estado} – aprobar o rechazar.</li>
 * </ol>
 *
 * <p>Todos los endpoints requieren un token JWT válido en el header
 * {@code Authorization: Bearer <token>}. El control de acceso granular
 * (qué reportes puede ver cada usuario, qué puede aprobar) se delega al
 * {@link ReporteService}, que extrae el {@code equipoId} y {@code equipoTipo}
 * directamente del token.</p>
 */
@RestController
@RequestMapping("/api/v1/reportes")
@Tag(name = "Reportes Mensuales", description = "Legalización de gastos mensuales por equipo")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    /**
     * Crea un nuevo reporte mensual con todos sus rubros de gasto en una sola operación.
     *
     * <p>El {@code equipoId} se toma del token JWT; el cliente no debe enviarlo.
     * El reporte se crea en estado {@code BORRADOR} hasta que se adjunte el soporte.</p>
     *
     * @param request temporada, mes, año y lista de rubros con sus montos
     * @return respuesta 201 con el reporte creado
     */
    @Operation(summary = "Crear reporte mensual",
               description = "Crea el cabezote y todos los rubros en una sola transacción. " +
                             "El equipoId se extrae del JWT.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<ReporteResponse> crearReporte(@RequestBody ReporteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reporteService.crearReporte(request));
    }

    /**
     * Adjunta el archivo de soporte (PDF/ZIP) a un reporte en estado BORRADOR
     * y lo avanza automáticamente a {@code PENDIENTE_ERLE}.
     *
     * <p>El archivo se recibe como {@code multipart/form-data} con el campo
     * {@code archivo}. Se almacena con el nombre
     * {@code SOPORTE_EQ{equipoId}_MES{mes}_{anio}.ext}.</p>
     *
     * @param id      ID del reporte al que se adjunta el soporte
     * @param archivo archivo PDF o ZIP de evidencia de gastos
     * @return respuesta 200 con el reporte actualizado (nuevo estado PENDIENTE_ERLE)
     */
    @Operation(summary = "Subir soporte del reporte",
               description = "Adjunta el archivo de evidencia y avanza el reporte a PENDIENTE_ERLE.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping(value = "/{id}/soporte", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReporteResponse> subirSoporte(
            @PathVariable Integer id,
            @RequestParam MultipartFile archivo) {
        return ResponseEntity.ok(reporteService.subirSoporte(id, archivo));
    }

    /**
     * Lista los reportes visibles para el usuario autenticado en una temporada dada.
     *
     * <p>El nivel de visibilidad se determina automáticamente por el {@code equipoTipo}
     * del token:</p>
     * <ul>
     *   <li><strong>ENL:</strong> todos los reportes del país.</li>
     *   <li><strong>ERLE:</strong> su propio reporte más los de sus ERL subordinados.</li>
     *   <li><strong>ERL:</strong> solo sus propios reportes.</li>
     * </ul>
     *
     * @param temporadaId ID de la temporada para filtrar
     * @return lista de reportes con sus detalles y montos totales
     */
    @Operation(summary = "Listar reportes",
               description = "Filtra automáticamente por jerarquía: ENL ve todo, " +
                             "ERLE ve su clúster, ERL solo el suyo.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<List<ReporteResponse>> listarReportes(
            @RequestParam Integer temporadaId) {
        return ResponseEntity.ok(reporteService.listarReportes(temporadaId));
    }

    /**
     * Cambia el estado de un reporte en el flujo de aprobación.
     *
     * <p>Transiciones permitidas:</p>
     * <ul>
     *   <li><strong>ERLE:</strong> {@code PENDIENTE_ERLE → PENDIENTE_ENL} o {@code RECHAZADO}.</li>
     *   <li><strong>ENL:</strong> {@code PENDIENTE_ENL → APROBADO} o {@code RECHAZADO}.</li>
     * </ul>
     *
     * <p>Al aprobar como ENL (estado {@code APROBADO}), los montos del reporte
     * empiezan a contar como ejecutado en las vistas financieras.</p>
     *
     * @param id      ID del reporte a modificar
     * @param request nuevo estado y observaciones (obligatorias si se rechaza)
     * @return respuesta 200 con el reporte en su nuevo estado
     */
    @Operation(summary = "Cambiar estado del reporte",
               description = "Aprobar o rechazar. Solo APROBADO actualiza el ejecutado financiero.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ReporteResponse> cambiarEstado(
            @PathVariable Integer id,
            @RequestBody CambiarEstadoRequest request) {
        return ResponseEntity.ok(reporteService.cambiarEstado(id, request));
    }

    /**
     * Maneja errores de validación de negocio (parámetros inválidos, duplicados, etc.).
     *
     * @param ex excepción lanzada por el servicio
     * @return respuesta 400 con el mensaje de error
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    /**
     * Maneja errores de estado inválido (permisos, transiciones no permitidas, etc.).
     *
     * @param ex excepción lanzada por el servicio
     * @return respuesta 409 Conflict con el mensaje de error
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }
}
