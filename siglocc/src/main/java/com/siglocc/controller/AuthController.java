package com.siglocc.controller;

import com.siglocc.dto.LoginRequest;
import com.siglocc.dto.LoginResponse;
import com.siglocc.entity.Usuario;
import com.siglocc.repository.UsuarioRepository;
import com.siglocc.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la autenticación de usuarios.
 *
 * <p>Expone el endpoint de login que permite a los usuarios obtener un token JWT
 * para acceder al resto de la API.</p>
 *
 * <p>Este controlador es el único que no requiere autenticación previa
 * (configurado como {@code permitAll} en {@link com.siglocc.config.SecurityConfig}).</p>
 *
 * <p><strong>Identidad jerárquica en el token:</strong> Además del email y rol,
 * el JWT incluye {@code equipoId} y {@code equipoTipo}. Estos claims son leídos
 * por el {@link com.siglocc.security.JwtAuthFilter} en cada request y almacenados
 * en el {@code SecurityContextHolder} para que el Dashboard pueda filtrar los datos
 * por nivel (ENL / ERLE / ERL) sin confiar en parámetros del cliente.</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Autentica un usuario con email y contraseña, y retorna un token JWT
     * con la identidad jerárquica completa del usuario.
     *
     * <p><strong>Pasos internos:</strong></p>
     * <ol>
     *   <li>Delega la autenticación al {@link AuthenticationManager}, que internamente
     *       llama a {@link com.siglocc.service.UserDetailsServiceImpl} y compara
     *       la contraseña contra el hash BCrypt en BD.</li>
     *   <li>Genera el JWT incluyendo: email (subject), rol, equipoId y equipoTipo.</li>
     *   <li>Retorna el token junto con los datos del usuario y su equipo.</li>
     * </ol>
     *
     * <p>Si las credenciales son incorrectas, Spring Security lanza
     * {@code BadCredentialsException} automáticamente y retorna HTTP 401.</p>
     *
     * @param request DTO con email y contraseña del usuario
     * @return HTTP 200 con token JWT e información completa del usuario autenticado
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        // Generar JWT con identidad jerárquica: rol + equipoId + tipo de equipo
        String token = jwtUtil.generateToken(
                usuario.getEmail(),
                usuario.getRol().getName(),
                usuario.getEquipo().getId(),
                usuario.getEquipo().getTipo().name()
        );

        LoginResponse.DetallesEquipo detallesEquipo = new LoginResponse.DetallesEquipo(
                usuario.getEquipo().getId(),
                usuario.getEquipo().getNombre(),
                usuario.getEquipo().getTipo().name(),
                usuario.getEquipo().getEnlId(),
                usuario.getEquipo().getErleId()
        );

        LoginResponse.UsuarioInfo usuarioInfo = new LoginResponse.UsuarioInfo(
                usuario.getId(),
                usuario.getName() + " " + usuario.getLastname(),
                usuario.getEmail(),
                usuario.getRol().getName(),
                usuario.getEquipo().getId(),
                usuario.getEquipo().getNombre(),
                detallesEquipo
        );

        return ResponseEntity.ok(new LoginResponse(token, "Bearer", usuarioInfo));
    }
}
