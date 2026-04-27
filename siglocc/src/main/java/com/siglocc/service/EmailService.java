package com.siglocc.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Servicio responsable del envío de correos electrónicos transaccionales.
 *
 * <p>Ofrece dos métodos de envío:</p>
 * <ul>
 *   <li>{@link #enviar} – texto plano (legacy), para usos simples.</li>
 *   <li>{@link #enviarHtml} – HTML con plantilla Thymeleaf, para correos visuales.</li>
 * </ul>
 *
 * <p>Ambos métodos están marcados con {@code @Async}, por lo que se ejecutan en
 * un hilo secundario del pool de Spring. Esto evita que el tiempo de conexión
 * SMTP bloquee el hilo principal y haga esperar al usuario del Front-end.</p>
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
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Envía un correo electrónico de texto plano de forma asíncrona.
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
            log.error("Error al enviar correo a {}: {}", destinatario, e.getMessage());
        }
    }

    /**
     * Envía un correo electrónico en formato HTML usando una plantilla Thymeleaf.
     *
     * <p>Las plantillas deben ubicarse en {@code src/main/resources/templates/email/}
     * con extensión {@code .html}. Las variables del mapa {@code variables} se
     * inyectan en la plantilla mediante el contexto de Thymeleaf.</p>
     *
     * @param destinatario dirección de correo del receptor
     * @param asunto       asunto del correo
     * @param plantilla    nombre de la plantilla sin ruta ni extensión
     *                     (ej: {@code "solicitud-aprobada"})
     * @param variables    variables que la plantilla necesita (nombre → valor)
     */
    @Async
    public void enviarHtml(String destinatario, String asunto,
                           String plantilla, Map<String, Object> variables) {
        try {
            Context ctx = new Context();
            ctx.setVariables(variables);
            String html = templateEngine.process("email/" + plantilla, ctx);

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(html, true);
            mailSender.send(mensaje);
            log.info("Correo HTML enviado a {}: {}", destinatario, asunto);
        } catch (Exception e) {
            log.error("Error al enviar correo HTML a {}: {}", destinatario, e.getMessage());
        }
    }
}
