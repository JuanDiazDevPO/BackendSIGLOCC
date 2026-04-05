package com.siglocc.config;

import com.siglocc.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración central de Spring Security para la aplicación SIGLOCC.
 *
 * <p>Define tres aspectos clave de la seguridad:</p>
 * <ol>
 *   <li><strong>Autenticación sin estado (stateless):</strong> No se usan sesiones HTTP.
 *       Cada request debe incluir un token JWT válido en el header
 *       {@code Authorization: Bearer <token>}.</li>
 *   <li><strong>Reglas de acceso por URL:</strong> El endpoint de login y los de
 *       Swagger son públicos. Todo lo demás requiere autenticación.</li>
 *   <li><strong>Seguridad a nivel de método:</strong> {@code @EnableMethodSecurity}
 *       activa el soporte de {@code @PreAuthorize} en los controladores, permitiendo
 *       restringir endpoints a roles específicos (ej: solo {@code ENL_RECURSOS} puede
 *       aprobar anticipos).</li>
 * </ol>
 *
 * <p>El flujo de autenticación es:</p>
 * <pre>
 *   Request HTTP
 *       │
 *       ▼
 *   JwtAuthFilter ── (valida token JWT) ──► SecurityContextHolder
 *       │
 *       ▼
 *   SecurityFilterChain ── (verifica permisos por URL)
 *       │
 *       ▼
 *   Controller → @PreAuthorize (verifica permisos por rol)
 * </pre>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, UserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Define la cadena de filtros de seguridad HTTP.
     *
     * <p>Configuraciones aplicadas:</p>
     * <ul>
     *   <li>CSRF deshabilitado: no aplica para APIs REST sin estado.</li>
     *   <li>Login por formulario deshabilitado: el login se maneja con JWT.</li>
     *   <li>HTTP Basic deshabilitado: se usa solo JWT.</li>
     *   <li>Sesiones en modo {@code STATELESS}: Spring no crea ni usa sesiones HTTP.</li>
     *   <li>Respuesta 401 (no autorizado) cuando se intenta acceder sin credenciales
     *       válidas, en lugar del 403 por defecto de Spring Security.</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()   // Login público
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()              // Preflight CORS
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()                                                        // Documentación pública
                .anyRequest().authenticated()                                        // Todo lo demás requiere token
            )
            .exceptionHandling(ex -> ex
                // Retornar 401 cuando el usuario no está autenticado (en lugar del 403 por defecto)
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendError(401, "No autorizado"))
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configura el proveedor de autenticación con el servicio de usuarios y
     * el encoder de contraseñas BCrypt.
     *
     * <p>Spring Security usa este proveedor durante el login para cargar el
     * usuario por email y comparar el password ingresado contra el hash en BD.</p>
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Expone el {@link AuthenticationManager} como bean para que pueda ser
     * inyectado en el {@link com.siglocc.controller.AuthController} y
     * ejecutar la autenticación manualmente durante el login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Bean del encoder de contraseñas.
     * BCrypt es el algoritmo estándar recomendado: aplica un salt aleatorio
     * y un factor de costo que lo hace resistente a ataques de fuerza bruta.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
