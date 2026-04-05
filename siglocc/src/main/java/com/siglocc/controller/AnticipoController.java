package com.siglocc.controller;

import com.siglocc.dto.AnticipoRequest;
import com.siglocc.dto.AnticipoResponse;
import com.siglocc.service.AnticipoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para el módulo de anticipos de presupuesto.
 *
 * <p>Expone dos endpoints:</p>
 * <ul>
 *   <li>{@code POST /api/v1/anticipos} – Crear una solicitud de anticipo.
 *       Accesible para cualquier usuario autenticado.</li>
 *   <li>{@code PATCH /api/v1/anticipos/{id}/aprobar} – Aprobar una solicitud pendiente.
 *       Solo accesible para el rol {@code ENL_RECURSOS}.</li>
 * </ul>
 *
 * <p>{@code @SecurityRequirement} le indica a Swagger que este controlador
 * requiere el token JWT (muestra el candado 🔒 en la documentación).</p>
 */
@RestController
@RequestMapping("/api/v1/anticipos")
@SecurityRequirement(name = "bearerAuth")
public class AnticipoController {

    private final AnticipoService anticipoService;

    public AnticipoController(AnticipoService anticipoService) {
        this.anticipoService = anticipoService;
    }

    /**
     * Crea una nueva solicitud de anticipo.
     *
     * <p>El sistema valida automáticamente el saldo disponible consultando
     * la vista {@code vista_control_saldos_enl}. El resultado puede ser:</p>
     * <ul>
     *   <li>HTTP 201 si el monto es válido y la solicitud queda en estado {@code PENDIENTE}.</li>
     *   <li>HTTP 400 si el monto supera el saldo disponible y la solicitud queda {@code RECHAZADO}.</li>
     * </ul>
     *
     * <p>El usuario, equipo y temporada se toman automáticamente de la sesión activa,
     * por lo que el Front-end solo necesita enviar: título, descripción, monto y tipo.</p>
     *
     * @param request datos de la solicitud ingresados por el usuario
     * @return HTTP 201 (pendiente) o HTTP 400 (rechazo automático)
     */
    @PostMapping
    public ResponseEntity<AnticipoResponse> crearSolicitud(@RequestBody AnticipoRequest request) {
        AnticipoResponse response = anticipoService.crearSolicitud(request);

        HttpStatus status = response.estado().equals("RECHAZADO")
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.CREATED;

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Aprueba una solicitud de anticipo en estado {@code PENDIENTE}.
     *
     * <p>Solo accesible para el rol {@code ENL_RECURSOS}. Al aprobarse,
     * el monto queda registrado como ejecutado en la vista de saldos y
     * se notifica al solicitante por correo electrónico.</p>
     *
     * @param id ID de la solicitud a aprobar
     * @return HTTP 200 con confirmación de la aprobación
     */
    @PatchMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ENL_RECURSOS')")
    public ResponseEntity<AnticipoResponse> aprobar(@PathVariable Integer id) {
        return ResponseEntity.ok(anticipoService.aprobarSolicitud(id));
    }

    /**
     * Maneja errores de validación: solicitud no encontrada o rol/equipo inexistente.
     * Retorna HTTP 400 con el mensaje del error.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    /**
     * Maneja errores de estado inválido (ej: intentar aprobar una solicitud ya aprobada).
     * Retorna HTTP 400 con el mensaje del error.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
