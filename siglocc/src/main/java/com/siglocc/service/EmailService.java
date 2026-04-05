package com.siglocc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Servicio responsable del envío de correos electrónicos transaccionales.
 *
 * <p>El método {@code enviar} está marcado con {@code @Async}, lo que significa
 * que se ejecuta en un hilo secundario del pool de Spring. Esto evita que el
 * tiempo de conexión con el servidor SMTP (típicamente 3-5 segundos con Gmail)
 * bloquee el hilo principal y haga esperar al usuario del Front-end.</p>
 *
 * <p><strong>Configuración SMTP</strong> (variables de entorno requeridas):</p>
 * <ul>
 *   <li>{@code MAIL_HOST}     – servidor SMTP (ej: {@code smtp.gmail.com})</li>
 *   <li>{@code MAIL_PORT}     – puerto SMTP (ej: {@code 587} para TLS)</li>
 *   <li>{@code MAIL_USERNAME} – cuenta de correo remitente</li>
 *   <li>{@code MAIL_PASSWORD} – contraseña de aplicación (no la contraseña personal)</li>
 * </ul>
 *
 * <p>Para Gmail se requiere una <em>App Password</em> (contraseña de aplicación),
 * generada en: Cuenta Google → Seguridad → Verificación en dos pasos →
 * Contraseñas de aplicaciones.</p>
 *
 * <p>Si el envío falla (red caída, credenciales incorrectas, etc.), el error
 * se registra en el log sin afectar la respuesta al usuario, ya que la
 * transacción de BD ya se completó.</p>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envía un correo electrónico de texto plano de forma asíncrona.
     *
     * <p>Al ser {@code @Async}, este método retorna inmediatamente al llamador
     * y el envío real ocurre en un hilo del pool de tareas de Spring.
     * {@code @EnableAsync} debe estar activo en la aplicación (ver
     * {@link com.siglocc.SigloccApplication}).</p>
     *
     * @param destinatario dirección de correo del receptor
     * @param asunto       asunto del correo
     * @param cuerpo       contenido del correo en texto plano
     */
    @Async
    public void enviar(String destinatario, String asunto, String cuerpo) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            mailSender.send(mensaje);
            log.info("Correo enviado a {}: {}", destinatario, asunto);
        } catch (Exception e) {
            // Se loguea el error pero no se propaga: el fallo de correo
            // no debe revertir ni interrumpir el flujo principal de negocio.
            log.error("Error al enviar correo a {}: {}", destinatario, e.getMessage());
        }
    }
}
