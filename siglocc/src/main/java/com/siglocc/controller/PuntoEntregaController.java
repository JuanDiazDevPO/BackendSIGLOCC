package com.siglocc.controller;

import com.siglocc.dto.PuntoEntregaRequest;
import com.siglocc.dto.PuntoEntregaResponse;
import com.siglocc.service.PuntoEntregaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de puntos de entrega logística.
 *
 * <p>Solo los equipos ERLE pueden registrar puntos de entrega. La respuesta
 * incluye la URL de Google Maps generada a partir de las coordenadas GPS.</p>
 */
@RestController
@RequestMapping("/api/v1/logistica/puntos-entrega")
@Tag(name = "Logística – Puntos de Entrega", description = "Registro de ubicaciones de distribución")
public class PuntoEntregaController {

    private final PuntoEntregaService puntoService;

    public PuntoEntregaController(PuntoEntregaService puntoService) {
        this.puntoService = puntoService;
    }

    /**
     * Registra un nuevo punto de entrega.
     * Solo disponible para equipos de tipo ERLE.
     */
    @Operation(summary = "Registrar punto de entrega",
               description = "Solo ERLE puede registrar puntos. La URL de Google Maps se genera " +
                             "automáticamente si se proporcionan coordenadas GPS.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<PuntoEntregaResponse> registrar(@RequestBody PuntoEntregaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(puntoService.registrar(request));
    }

    /**
     * Lista los puntos de entrega visibles para el usuario autenticado en una temporada.
     * ENL ve todos; ERLE ve su clúster; ERL ve los de su equipo.
     */
    @Operation(summary = "Listar puntos de entrega",
               description = "Filtrado automático por jerarquía del JWT.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<List<PuntoEntregaResponse>> listar(@RequestParam Integer temporadaId) {
        return ResponseEntity.ok(puntoService.listar(temporadaId));
    }

}
