package com.siglocc.controller;

import com.siglocc.dto.RegistroRequest;
import com.siglocc.dto.UsuarioResponse;
import com.siglocc.service.UsuarioService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para la gestión de usuarios.
 *
 * <p>Actualmente expone el endpoint de registro de nuevos usuarios.
 * Solo los roles {@code ENL_RECURSOS} y {@code ENL_LOGISTICA} tienen
 * permiso para crear usuarios; cualquier otro rol recibirá HTTP 403.</p>
 *
 * <p>{@code @SecurityRequirement} le indica a Swagger que este controlador
 * requiere el token JWT (muestra el candado 🔒 en la documentación).</p>
 */
@RestController
@RequestMapping("/api/usuarios")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * <p>Solo accesible para usuarios con rol {@code ENL_RECURSOS} o
     * {@code ENL_LOGISTICA}. La contraseña se hashea automáticamente
     * con BCrypt en el servicio antes de persistirse.</p>
     *
     * @param request datos del nuevo usuario (nombre, apellido, email, contraseña, rol, equipo)
     * @return HTTP 201 con los datos del usuario creado (sin contraseña)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ENL_RECURSOS', 'ENL_LOGISTICA')")
    public ResponseEntity<UsuarioResponse> registrar(@RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrar(request));
    }

    /**
     * Maneja errores de validación de negocio (email duplicado, rol/equipo inexistente).
     * Retorna HTTP 400 con un mensaje descriptivo del error.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
