package com.siglocc.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de seguridad que intercepta cada request HTTP para validar el token JWT
 * e inyectar la identidad jerárquica del usuario en el contexto de seguridad.
 *
 * <p>Se ejecuta una sola vez por request (extiende {@link OncePerRequestFilter}).
 * Es el componente que conecta el token JWT con el contexto de seguridad de Spring.</p>
 *
 * <p><strong>Flujo de procesamiento:</strong></p>
 * <ol>
 *   <li>Extrae el header {@code Authorization} del request.</li>
 *   <li>Si no existe o no empieza con {@code "Bearer "}, deja pasar el request sin autenticar.</li>
 *   <li>Extrae el token y obtiene el email del usuario desde el payload JWT.</li>
 *   <li>Carga el usuario desde la BD y valida el token.</li>
 *   <li>Si es válido, extrae los claims {@code equipoId} y {@code equipoTipo} del token
 *       y los empaqueta en un {@link JwtAuthDetails}, que se adjunta como {@code details}
 *       al objeto de autenticación en el {@link SecurityContextHolder}.</li>
 *   <li>Cualquier excepción (token expirado, malformado, etc.) se captura silenciosamente:
 *       el request continúa sin autenticación y Spring Security lo rechazará si el
 *       endpoint lo requiere.</li>
 * </ol>
 *
 * <p><strong>Por qué almacenar equipoId/equipoTipo en los details:</strong>
 * Los servicios del Dashboard necesitan saber qué nivel jerárquico tiene el usuario
 * para filtrar los datos. Al extraerlos del JWT (firmado por el servidor), se garantiza
 * que el filtrado no puede ser manipulado desde el cliente.</p>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Si no hay token, continúa la cadena de filtros sin autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7); // Quita el prefijo "Bearer "
            String username = jwtUtil.extractUsername(token);

            // Solo autentica si hay usuario en el token y aún no hay autenticación en el contexto
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.isTokenValid(token, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    // Inyectar identidad jerárquica extraída del JWT (no del cliente).
                    // DashboardService usará estos datos para filtrar por nivel ENL/ERLE/ERL.
                    authToken.setDetails(new JwtAuthDetails(
                            jwtUtil.extractEquipoId(token),
                            jwtUtil.extractEquipoTipo(token)
                    ));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token inválido o expirado: se continúa sin autenticación.
            // Spring Security rechazará el acceso si el endpoint lo requiere.
        }

        chain.doFilter(request, response);
    }
}
