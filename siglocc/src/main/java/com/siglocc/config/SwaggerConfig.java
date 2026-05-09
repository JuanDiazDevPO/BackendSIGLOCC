package com.siglocc.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la documentación de la API con SpringDoc OpenAPI (Swagger).
 *
 * <p>
 * Hace dos cosas principales:
 * </p>
 * <ol>
 * <li><strong>Define el esquema de seguridad JWT:</strong> La anotación
 * {@code @SecurityScheme} registra "bearerAuth" como el mecanismo de
 * autenticación. Esto hace aparecer el botón <em>"Authorize"</em> en la
 * interfaz de Swagger UI, donde el desarrollador puede pegar su token JWT
 * para probar los endpoints protegidos sin necesidad de un cliente
 * externo.</li>
 * <li><strong>Personaliza la información de la API:</strong> Título,
 * descripción
 * y versión que aparecen en la cabecera de Swagger UI.</li>
 * </ol>
 *
 * <p>
 * Los endpoints de Swagger están permitidos sin autenticación en
 * {@link SecurityConfig} ({@code /swagger-ui/**, /v3/api-docs/**}).
 * </p>
 *
 * <p>
 * Acceso: {@code http://localhost:8080/swagger-ui/index.html}
 * </p>
 */
@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class SwaggerConfig {

        /**
         * Configura los metadatos generales de la documentación OpenAPI.
         */
        @Bean
        public OpenAPI openAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("SIGLOCC API")
                                                .description("Backend del Sistema de Gestión de Logística")
                                                .version("1.0"))
                                .addServersItem(new io.swagger.v3.oas.models.servers.Server()
                                                .url("/")
                                                .description("Servidor actual"));
        }
}
