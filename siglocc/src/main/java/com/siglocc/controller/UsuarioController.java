package com.siglocc.controller;

import com.siglocc.dto.EquipoItemResponse;
import com.siglocc.dto.RegistroRequest;
import com.siglocc.dto.RolResponse;
import com.siglocc.dto.UsuarioResponse;
import com.siglocc.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de usuarios.
 *
 * <p>Expone varios endpoints, todos restringidos a {@code ENL_RECURSOS} y {@code ENL_LOGISTICA}:</p>
 * <ul>
 *   <li>{@code GET  /api/usuarios}         – Listar todos los usuarios (activos e inactivos).</li>
 *   <li>{@code POST /api/usuarios}         – Registrar nuevo usuario.</li>
 *   <li>{@code PATCH /api/usuarios/{id}/inactivar} – Inactivar usuario.</li>
 *   <li>{@code PATCH /api/usuarios/{id}/activar}   – Reactivar usuario.</li>
 *   <li>{@code GET  /api/usuarios/roles}   – Listar roles disponibles.</li>
 *   <li>{@code GET  /api/usuarios/equipos} – Listar equipos disponibles.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/usuarios")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuarios", description = "Gestión de usuarios del sistema")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Retorna todos los usuarios registrados en el sistema, activos e inactivos.
     *
     * <p>Solo accesible para {@code ENL_RECURSOS} o {@code ENL_LOGISTICA}, igual
     * que el resto de la gestión de usuarios.</p>
     *
     * @return HTTP 200 con la lista completa de usuarios
     */
    @Operation(summary = "Listar usuarios", description = "Devuelve todos los usuarios del sistema, activos e inactivos, ordenados por equipo.")
    @GetMapping
    @PreAuthorize("hasAnyRole('ENL_RECURSOS', 'ENL_LOGISTICA')")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listarUsuarios());
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
     * Inactiva un usuario impidiendo que pueda iniciar sesión.
     */
    @Operation(summary = "Inactivar usuario", description = "Bloquea el acceso del usuario sin eliminarlo de la BD.")
    @PatchMapping("/{id}/inactivar")
    @PreAuthorize("hasAnyRole('ENL_RECURSOS', 'ENL_LOGISTICA')")
    public ResponseEntity<Map<String, String>> inactivar(@PathVariable Integer id) {
        usuarioService.inactivarUsuario(id);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario inactivado correctamente."));
    }

    /**
     * Reactiva un usuario permitiéndole volver a iniciar sesión.
     */
    @Operation(summary = "Activar usuario", description = "Restaura el acceso de un usuario previamente inactivado.")
    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ENL_RECURSOS', 'ENL_LOGISTICA')")
    public ResponseEntity<Map<String, String>> activar(@PathVariable Integer id) {
        usuarioService.activarUsuario(id);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario activado correctamente."));
    }

    /**
     * Retorna todos los roles disponibles para asignar al registrar un usuario.
     */
    @Operation(summary = "Listar roles", description = "Devuelve todos los roles del sistema.")
    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('ENL_RECURSOS', 'ENL_LOGISTICA')")
    public ResponseEntity<List<RolResponse>> listarRoles() {
        return ResponseEntity.ok(usuarioService.listarRoles());
    }

    /**
     * Retorna todos los equipos disponibles para asignar al registrar un usuario.
     */
    @Operation(summary = "Listar equipos", description = "Devuelve todos los equipos con id, nombre y tipo jerárquico.")
    @GetMapping("/equipos")
    @PreAuthorize("hasAnyRole('ENL_RECURSOS', 'ENL_LOGISTICA')")
    public ResponseEntity<List<EquipoItemResponse>> listarEquipos() {
        return ResponseEntity.ok(usuarioService.listarEquipos());
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
