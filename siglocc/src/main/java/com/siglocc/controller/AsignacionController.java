package com.siglocc.controller;

import com.siglocc.dto.AjusteAsignacionRequest;
import com.siglocc.dto.AsignacionResponse;
import com.siglocc.security.JwtAuthDetails;
import com.siglocc.service.AsignacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST del motor de asignación inteligente de cajas y literatura.
 *
 * <p>Expone cuatro endpoints:</p>
 * <ol>
 *   <li>{@code POST  /api/v1/logistica/asignaciones/generar?temporadaId=} –
 *       genera una corrida automática (Método Hamilton) en estado BORRADOR.</li>
 *   <li>{@code PATCH /api/v1/logistica/asignaciones/{id}/ajustar} –
 *       ajusta manualmente una línea de asignación.</li>
 *   <li>{@code PATCH /api/v1/logistica/asignaciones/{id}/confirmar} –
 *       confirma la corrida y descuenta el inventario.</li>
 *   <li>{@code GET   /api/v1/logistica/asignaciones?temporadaId=} –
 *       lista corridas anteriores con jerarquía ENL/ERLE/ERL.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/logistica/asignaciones")
@Tag(name = "Logística – Asignación Inteligente",
     description = "Distribución automática de cajas y literatura (Método Hamilton)")
public class AsignacionController {

    private final AsignacionService asignacionService;

    public AsignacionController(AsignacionService asignacionService) {
        this.asignacionService = asignacionService;
    }

    /**
     * Genera la distribución automática de cajas y literatura para el equipo
     * del usuario autenticado.
     *
     * <p>El equipoId se extrae del JWT. El sistema distribuye el inventario disponible
     * de forma proporcional entre todas las iglesias APROBADAS con cajas solicitadas.
     * La corrida queda en estado BORRADOR para revisión y ajuste manual.</p>
     *
     * @param temporadaId temporada para la que se genera la asignación
     * @return la corrida generada con todos sus detalles
     */
    @Operation(summary = "Generar asignación automática",
               description = "Distribuye el stock disponible proporcionalmente entre iglesias " +
                             "APROBADAS usando el Método Hamilton. El equipoId se extrae del JWT. " +
                             "Resultado en estado BORRADOR.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/generar")
    public ResponseEntity<AsignacionResponse> generar(@RequestParam Integer temporadaId) {
        Integer equipoId = obtenerEquipoId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(asignacionService.generarAsignacionAutomatica(equipoId, temporadaId));
    }

    /**
     * Ajusta manualmente la cantidad asignada a una iglesia para una categoría de caja
     * o tipo de literatura, mientras la corrida esté en BORRADOR.
     *
     * @param id      ID de la cabecera de asignación
     * @param request datos del ajuste
     * @return la asignación completa actualizada
     */
    @Operation(summary = "Ajustar línea de asignación",
               description = "Modifica la cantidad de una línea específica (iglesia × categoría/ítem). " +
                             "Solo disponible en estado BORRADOR.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/ajustar")
    public ResponseEntity<AsignacionResponse> ajustar(
            @PathVariable Integer id,
            @RequestBody AjusteAsignacionRequest request) {
        return ResponseEntity.ok(asignacionService.ajustarLinea(id, request));
    }

    /**
     * Confirma la corrida de asignación.
     *
     * <p>Cambia el estado de BORRADOR a CONFIRMADA. A partir de este momento el
     * inventario queda descontado y no se permiten más ajustes manuales.</p>
     *
     * @param id ID de la cabecera a confirmar
     * @return la asignación en estado CONFIRMADA
     */
    @Operation(summary = "Confirmar asignación",
               description = "Cierra la corrida (BORRADOR → CONFIRMADA) y descuenta el inventario. " +
                             "No se puede deshacer.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<AsignacionResponse> confirmar(@PathVariable Integer id) {
        return ResponseEntity.ok(asignacionService.confirmar(id));
    }

    /**
     * Lista las corridas de asignación de una temporada visibles para el usuario autenticado.
     *
     * @param temporadaId temporada a consultar
     * @return lista de corridas ordenadas por fecha descendente
     */
    @Operation(summary = "Listar corridas de asignación",
               description = "ENL ve todas; ERLE ve su clúster; ERL ve las suyas. " +
                             "Incluye detalles completos de cada corrida.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<List<AsignacionResponse>> listar(@RequestParam Integer temporadaId) {
        return ResponseEntity.ok(asignacionService.listar(temporadaId));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private Integer obtenerEquipoId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getDetails() instanceof JwtAuthDetails details) {
            return details.equipoId();
        }
        throw new IllegalStateException("El token no contiene identidad jerárquica válida.");
    }
}
