package com.siglocc.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utilidad de desarrollo para generar hashes BCrypt de contraseñas.
 *
 * <p><strong>IMPORTANTE:</strong> Esta clase es solo para uso en desarrollo local.
 * No debe desplegarse en producción ni incluirse en el build final.</p>
 *
 * <p><strong>Cómo usarla:</strong></p>
 * <ol>
 *   <li>Agrega las contraseñas en texto plano al arreglo {@code passwords}.</li>
 *   <li>Ejecuta la clase como Java Application (clic derecho → Run As → Java Application).</li>
 *   <li>Copia el hash generado en la consola.</li>
 *   <li>Úsalo en un INSERT o UPDATE de la tabla {@code usuarios}:
 *       <pre>UPDATE usuarios SET password = '$2a$10$...' WHERE email = 'x@y.com';</pre>
 *   </li>
 * </ol>
 *
 * <p>El hash BCrypt tiene siempre 60 caracteres y comienza con {@code $2a$10$}.
 * Si el hash guardado en BD no tiene ese formato, Spring Security lo rechazará
 * con el warning "Encoded password does not look like BCrypt".</p>
 */
public class GenerarHash {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        // Agrega aquí las contraseñas que necesitas hashear
        String[] passwords = {"region123"};
        for (String password : passwords) {
            System.out.println(password + " -> " + encoder.encode(password));
        }
    }
}
