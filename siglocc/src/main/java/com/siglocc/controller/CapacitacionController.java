package com.siglocc.controller;

import com.siglocc.dto.CapacitacionRequest;
import com.siglocc.dto.CapacitacionResponse;
import com.siglocc.service.CapacitacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST del Momento 2 – La Capacitación.
 *
 * <p>Registra las jornadas de capacitación de maestros de iglesias aprobadas.
 * El número de cajas se calcula automáticamente: {@code maestrosEnviados × 25}.</p>
 */
@RestController
@RequestMapping("/api/v1/logistica/capacitaciones")
@Tag(name = "Logística – Momento 2 Capacitación", description = "Registro de capacitación de maestros")
public class CapacitacionController {

    private final CapacitacionService capacitacionService;

    public CapacitacionController(CapacitacionService capacitacionService) {
        this.capacitacionService = capacitacionService;
    }

    /**
     * Registra la capacitación de los maestros de una iglesia aprobada.
     * Las cajas calculadas = maestrosEnviados × 25.
     */
    @Operation(summary = "Registrar capacitación",
               description = "Solo iglesias en estado APROBADA pueden registrar capacitación. " +
                             "cajasCalculadas = maestrosEnviados × 25.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<CapacitacionResponse> registrar(@RequestBody CapacitacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(capacitacionService.registrar(request));
    }

    /**
     * Lista las capacitaciones visibles para el usuario autenticado en una temporada.
     */
    @Operation(summary = "Listar capacitaciones",
               description = "Filtrado automático por jerarquía del JWT.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<List<CapacitacionResponse>> listar(@RequestParam Integer temporadaId) {
        return ResponseEntity.ok(capacitacionService.listar(temporadaId));
    }

}
