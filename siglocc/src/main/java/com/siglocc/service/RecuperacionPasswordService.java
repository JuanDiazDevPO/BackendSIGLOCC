package com.siglocc.service;

import com.siglocc.entity.TokenRecuperacion;
import com.siglocc.entity.Usuario;
import com.siglocc.repository.TokenRecuperacionRepository;
import com.siglocc.repository.UsuarioRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio que gestiona el proceso completo de restablecimiento de contraseña.
 *
 * <p>Implementa dos operaciones que conforman el flujo de recuperación:</p>
 * <ol>
 *   <li>{@link #solicitarRecuperacion} – Genera el token, lo persiste y envía
 *       el enlace de recuperación por correo.</li>
 *   <li>{@link #restablecerPassword} – Valida el token y actualiza la contraseña
 *       con el nuevo valor cifrado en BCrypt.</li>
 * </ol>
 *
 * <p><strong>Consideraciones de seguridad implementadas:</strong></p>
 * <ul>
 *   <li>La respuesta de {@link #solicitarRecuperacion} es siempre la misma
 *       sin importar si el email existe, evitando la enumeración de usuarios.</li>
 *   <li>Cada nueva solicitud elimina tokens anteriores del mismo usuario,
 *       invalidando enlaces viejos que pudieran seguir en la bandeja del correo.</li>
 *   <li>El token tiene una ventana de 30 minutos y se marca como usado tras
 *       el restablecimiento, impidiendo su reutilización.</li>
 *   <li>El token se genera con {@code UUID.randomUUID()}, que produce 122 bits
 *       de aleatoriedad criptográfica.</li>
 * </ul>
 *
 * <p><strong>Configuración requerida en {@code application.properties}:</strong></p>
 * <pre>
 * app.frontend.url=https://tu-dominio.com
 * </pre>
 * Si no se configura, el valor por defecto es {@code http://localhost:4200}
 * (servidor de desarrollo Angular).
 */
@Service
public class RecuperacionPasswordService {

    private final TokenRecuperacionRepository tokenRepo;
    private final UsuarioRepository usuarioRepo;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    /**
     * URL base del Front-end. Se usa para construir el enlace del correo.
     * Configurable en {@code application.properties} como {@code app.frontend.url}.
     */
    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "TokenRecuperacionRepository es un bean singleton gestionado por Spring; no es posible ni necesario hacer copia defensiva.")
    public RecuperacionPasswordService(TokenRecuperacionRepository tokenRepo,
                                       UsuarioRepository usuarioRepo,
                                       EmailService emailService,
                                       PasswordEncoder passwordEncoder) {
        this.tokenRepo = tokenRepo;
        this.usuarioRepo = usuarioRepo;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Inicia el proceso de recuperación de contraseña para el email indicado.
     *
     * <p><strong>Flujo interno:</strong></p>
     * <ol>
     *   <li>Busca el usuario por email en BD.</li>
     *   <li>Si no existe, retorna sin hacer nada (respuesta idéntica para no
     *       revelar si el email está registrado).</li>
     *   <li>Elimina cualquier token previo del usuario (invalida enlaces anteriores).</li>
     *   <li>Genera un nuevo UUID como token y lo persiste con expiración de 30 minutos.</li>
     *   <li>Envía el correo de forma asíncrona con el enlace de recuperación.</li>
     * </ol>
     *
     * @param email correo electrónico del usuario que solicita la recuperación
     */
    @Transactional
    public void solicitarRecuperacion(String email) {
        // Buscar usuario; si no existe, salir silenciosamente para no revelar
        // que el email no está registrado (prevención de enumeración de usuarios)
        usuarioRepo.findByEmail(email).ifPresent(usuario -> {

            // Eliminar tokens anteriores de este usuario para que solo el nuevo enlace sea válido
            tokenRepo.deleteByUsuarioId(usuario.getId());

            // Generar token UUID único de 122 bits de aleatoriedad
            String tokenValor = UUID.randomUUID().toString();

            TokenRecuperacion tokenRecuperacion = new TokenRecuperacion();
            tokenRecuperacion.setUsuarioId(usuario.getId());
            tokenRecuperacion.setToken(tokenValor);
            tokenRecuperacion.setExpiracion(LocalDateTime.now().plusMinutes(30));
            tokenRecuperacion.setUsado(false);
            tokenRepo.save(tokenRecuperacion);

            // Construir el enlace que el usuario recibirá en su correo
            String enlace = frontendUrl + "/reset-password?token=" + tokenValor;

            // Enviar correo de forma asíncrona (no bloquea la respuesta HTTP)
            emailService.enviarHtml(
                    usuario.getEmail(),
                    "SIGLOCC - Restablecimiento de contraseña",
                    "recuperacion-password",
                    Map.of(
                        "nombre", usuario.getName(),
                        "enlace", enlace
                    )
            );
        });
    }

    /**
     * Valida el token de recuperación y actualiza la contraseña del usuario.
     *
     * <p><strong>Validaciones aplicadas en orden:</strong></p>
     * <ol>
     *   <li>El token debe existir en BD.</li>
     *   <li>El token no debe haber expirado (máximo 30 minutos desde su creación).</li>
     *   <li>El token no debe haber sido usado previamente.</li>
     * </ol>
     *
     * <p>Si todas las validaciones pasan, la nueva contraseña se cifra con BCrypt
     * y se guarda en la tabla {@code usuarios}. El token se marca como usado.</p>
     *
     * @param tokenValor    UUID recibido desde el enlace del correo
     * @param nuevaPassword nueva contraseña elegida por el usuario (en texto plano)
     * @throws IllegalArgumentException si el token no existe, está expirado o ya fue usado
     */
    @Transactional
    public void restablecerPassword(String tokenValor, String nuevaPassword) {
        TokenRecuperacion tokenRecuperacion = tokenRepo.findByToken(tokenValor)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El enlace de recuperación no es válido."));

        if (LocalDateTime.now().isAfter(tokenRecuperacion.getExpiracion())) {
            throw new IllegalArgumentException(
                    "El enlace de recuperación ha expirado. Solicita uno nuevo.");
        }

        if (tokenRecuperacion.getUsado()) {
            throw new IllegalArgumentException(
                    "Este enlace ya fue utilizado. Solicita uno nuevo si necesitas cambiar tu contraseña.");
        }

        // Actualizar contraseña con hash BCrypt
        Usuario usuario = usuarioRepo.findById(tokenRecuperacion.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepo.save(usuario);

        // Invalidar el token para que no pueda reutilizarse
        tokenRecuperacion.setUsado(true);
        tokenRepo.save(tokenRecuperacion);
    }
}
