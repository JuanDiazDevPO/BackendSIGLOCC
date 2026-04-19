package com.siglocc.controller;

import com.siglocc.dto.TemporadaResponse;
import com.siglocc.entity.Temporada;
import com.siglocc.repository.TemporadaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para consulta de temporadas operativas.
 *
 * <p>Expone dos endpoints de solo lectura:</p>
 * <ol>
 *   <li>{@code GET /api/v1/temporadas} – lista todas las temporadas.</li>
 *   <li>{@code GET /api/v1/temporadas/actual} – devuelve la temporada activa.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/temporadas")
@Tag(name = "Temporadas", description = "Consulta de temporadas operativas")
public class TemporadaController {

    private final TemporadaRepository temporadaRepo;

    public TemporadaController(TemporadaRepository temporadaRepo) {
        this.temporadaRepo = temporadaRepo;
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

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private TemporadaResponse toResponse(Temporada t) {
        return new TemporadaResponse(t.getId(), t.getNombre(),
                t.getFechaInicio(), t.getFechaFin(), t.isEsActual());
    }
}
