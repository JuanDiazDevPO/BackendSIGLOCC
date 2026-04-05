package com.siglocc;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Configuración para el despliegue de la aplicación como archivo WAR
 * en un servidor de aplicaciones externo (por ejemplo, Tomcat, WildFly).
 *
 * <p>Cuando la aplicación se despliega como WAR en lugar de ejecutarse con
 * el servidor embebido, el contenedor de servlets necesita un punto de entrada
 * estándar. Esta clase extiende {@link SpringBootServletInitializer} y le
 * indica a Spring Boot qué clase principal debe usar para arrancar.</p>
 *
 * <p>En desarrollo local se puede ignorar esta clase; entra en juego solo
 * en entornos de producción con servidor externo.</p>
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SigloccApplication.class);
    }
}
