package com.siglocc.controller;

import com.siglocc.dto.LoginRequest;
import com.siglocc.dto.LoginResponse;
import com.siglocc.dto.RecuperarPasswordRequest;
import com.siglocc.dto.RestablecerPasswordRequest;
import com.siglocc.entity.Usuario;
import com.siglocc.repository.UsuarioRepository;
import com.siglocc.security.JwtUtil;
import com.siglocc.service.RecuperacionPasswordService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para la autenticación y gestión de acceso de usuarios.
 *
 * <p>Todos los endpoints de este controlador son públicos (no requieren token JWT)
 * ya que son el punto de entrada al sistema.</p>
 *
 * <p>Expone tres operaciones:</p>
 * <ul>
 *   <li>{@code POST /api/auth/login} – Autenticar con email y contraseña.</li>
 *   <li>{@code POST /api/auth/recuperar-password} – Solicitar enlace de recuperación.</li>
 *   <li>{@code POST /api/auth/restablecer-password} – Confirmar nueva contraseña con el token.</li>
 * </ul>
 *
 * <p><strong>Identidad jerárquica en el token:</strong> El JWT generado en el login
 * incluye {@code equipoId} y {@code equipoTipo}, que son leídos por
 * {@link com.siglocc.security.JwtAuthFilter} para determinar qué datos puede
 * ver cada usuario en el Dashboard sin confiar en parámetros del cliente.</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;
    private final RecuperacionPasswordService recuperacionService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UsuarioRepository usuarioRepository,
                          RecuperacionPasswordService recuperacionService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
        this.recuperacionService = recuperacionService;
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

    /**
     * Inicia el proceso de recuperación de contraseña enviando un enlace al correo.
     *
     * <p>La respuesta es siempre {@code 200 OK} con el mismo mensaje, independientemente
     * de si el email existe en BD o no. Esto evita que un atacante pueda determinar
     * qué correos están registrados en el sistema (prevención de enumeración de usuarios).</p>
     *
     * <p>Si el email existe, el usuario recibirá un enlace válido por 30 minutos.
     * El envío del correo es asíncrono y no bloquea la respuesta HTTP.</p>
     *
     * @param request DTO con el email del usuario que olvidó su contraseña
     * @return HTTP 200 con mensaje genérico de confirmación
     */
    @PostMapping("/recuperar-password")
    public ResponseEntity<Map<String, String>> recuperarPassword(
            @RequestBody RecuperarPasswordRequest request) {
        recuperacionService.solicitarRecuperacion(request.email());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Si el correo está registrado, recibirás un enlace para restablecer tu contraseña."
        ));
    }

    /**
     * Confirma el restablecimiento de contraseña usando el token del enlace del correo.
     *
     * <p>El token se extrae del parámetro {@code ?token=...} de la URL del enlace y se
     * envía junto con la nueva contraseña elegida por el usuario.</p>
     *
     * <p>El servicio valida que el token exista, no haya expirado y no haya sido
     * usado previamente. Si alguna condición falla, retorna HTTP 400 con el motivo.</p>
     *
     * @param request DTO con el token UUID y la nueva contraseña en texto plano
     * @return HTTP 200 si el restablecimiento fue exitoso, HTTP 400 si el token es inválido
     */
    @PostMapping("/restablecer-password")
    public ResponseEntity<Map<String, String>> restablecerPassword(
            @RequestBody RestablecerPasswordRequest request) {
        recuperacionService.restablecerPassword(request.token(), request.nuevaPassword());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Contraseña restablecida exitosamente. Ya puedes iniciar sesión."
        ));
    }

    /**
     * Maneja errores de validación del token (expirado, ya usado, no encontrado).
     * Retorna HTTP 400 con el mensaje descriptivo del problema.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
