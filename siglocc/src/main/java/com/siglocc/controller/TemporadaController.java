package com.siglocc.controller;

import com.siglocc.dto.TemporadaRequest;
import com.siglocc.dto.TemporadaResponse;
import com.siglocc.entity.Temporada;
import com.siglocc.repository.TemporadaRepository;
import com.siglocc.service.TemporadaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de temporadas operativas.
 *
 * <p>Expone cinco endpoints:</p>
 * <ol>
 *   <li>{@code GET   /api/v1/temporadas} – lista todas las temporadas. Cualquier autenticado.</li>
 *   <li>{@code GET   /api/v1/temporadas/actual} – devuelve la temporada activa. Cualquier autenticado.</li>
 *   <li>{@code POST  /api/v1/temporadas} – crea una nueva. Solo ENL_RECURSOS/ENL_LOGISTICA.</li>
 *   <li>{@code PUT   /api/v1/temporadas/{id}} – edita nombre y fechas. Solo ENL_RECURSOS/ENL_LOGISTICA.</li>
 *   <li>{@code PATCH /api/v1/temporadas/{id}/activar} – la marca como actual, desactivando
 *       cualquier otra. Solo ENL_RECURSOS/ENL_LOGISTICA.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/temporadas")
@Tag(name = "Temporadas", description = "Consulta y gestión de temporadas operativas")
public class TemporadaController {

    private final TemporadaRepository temporadaRepo;
    private final TemporadaService temporadaService;

    public TemporadaController(TemporadaRepository temporadaRepo, TemporadaService temporadaService) {
        this.temporadaRepo = temporadaRepo;
        this.temporadaService = temporadaService;
    }

    /**
     * Devuelve todas las temporadas registradas en el sistema, ordenadas por ID.
     */
    @Operation(summary = "Listar todas las temporadas",
               description = "Retorna el listado completo de temporadas con sus fechas y estado de actividad.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<List<TemporadaResponse>> listar() {
        List<TemporadaResponse> resultado = temporadaRepo.findAll().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(resultado);
    }

    /**
     * Devuelve la temporada actualmente activa ({@code es_actual = true}).
     *
     * @throws IllegalStateException si no hay ninguna temporada marcada como actual
     */
    @Operation(summary = "Obtener temporada actual",
               description = "Retorna la temporada con es_actual = true. " +
                             "Útil para que el frontend pre-seleccione la temporada vigente.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/actual")
    public ResponseEntity<TemporadaResponse> obtenerActual() {
        Temporada actual = temporadaRepo.findByEsActualTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay ninguna temporada marcada como actual en el sistema."));
        return ResponseEntity.ok(toResponse(actual));
    }

    /**
     * Crea una nueva temporada. Nace siempre inactiva ({@code esActual = false});
     * activarla es un paso aparte con {@code PATCH /{id}/activar}.
     *
     * @param request nombre, fecha de inicio y fecha de fin
     * @return HTTP 201 con la temporada creada
     */
    @Operation(summary = "Crear temporada",
               description = "Crea una nueva temporada, siempre inactiva. Solo ENL_RECURSOS/ENL_LOGISTICA.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ENL_RECURSOS', 'ENL_LOGISTICA')")
    @PostMapping
    public ResponseEntity<TemporadaResponse> crear(@RequestBody TemporadaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(temporadaService.crear(request));
    }

    /**
     * Edita el nombre y las fechas de una temporada existente. No cambia cuál está activa.
     *
     * @param id      ID de la temporada a editar
     * @param request nuevo nombre, fecha de inicio y fecha de fin
     * @return HTTP 200 con la temporada actualizada
     */
    @Operation(summary = "Editar temporada",
               description = "Actualiza nombre y fechas. No afecta esActual. Solo ENL_RECURSOS/ENL_LOGISTICA.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ENL_RECURSOS', 'ENL_LOGISTICA')")
    @PutMapping("/{id}")
    public ResponseEntity<TemporadaResponse> editar(
            @PathVariable Integer id,
            @RequestBody TemporadaRequest request) {
        return ResponseEntity.ok(temporadaService.editar(id, request));
    }

    /**
     * Marca una temporada como la actual, desactivando automáticamente
     * cualquier otra que lo estuviera. Operación atómica.
     *
     * @param id ID de la temporada a activar
     * @return HTTP 200 con la temporada ya activa
     */
    @Operation(summary = "Activar temporada",
               description = "La marca como esActual = true y desactiva la que estuviera activa antes, " +
                             "en la misma transacción. Solo ENL_RECURSOS/ENL_LOGISTICA.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ENL_RECURSOS', 'ENL_LOGISTICA')")
    @PatchMapping("/{id}/activar")
    public ResponseEntity<TemporadaResponse> activar(@PathVariable Integer id) {
        return ResponseEntity.ok(temporadaService.activar(id));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private TemporadaResponse toResponse(Temporada t) {
        return new TemporadaResponse(t.getId(), t.getNombre(),
                t.getFechaInicio(), t.getFechaFin(), t.isEsActual());
    }
}
