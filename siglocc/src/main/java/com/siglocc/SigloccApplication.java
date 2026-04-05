package com.siglocc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Clase principal de la aplicación SIGLOCC.
 *
 * <p>Punto de entrada del backend. Al arrancarse, Spring Boot escanea todos los
 * componentes del paquete {@code com.siglocc} y sus subpaquetes, configura la
 * conexión a la base de datos, levanta el servidor embebido Tomcat y expone los
 * endpoints REST.</p>
 *
 * <p>{@code @EnableAsync} habilita el soporte de ejecución asíncrona en toda la
 * aplicación. Es necesario para que los métodos marcados con {@code @Async}
 * (por ejemplo, el envío de correos en {@link com.siglocc.service.EmailService})
 * se ejecuten en un hilo secundario sin bloquear el hilo principal.</p>
 */
@SpringBootApplication
@EnableAsync
public class SigloccApplication {

    public static void main(String[] args) {
        SpringApplication.run(SigloccApplication.class, args);
    }
}
