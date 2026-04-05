package com.siglocc.service;

import com.siglocc.entity.Usuario;
import com.siglocc.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementación de {@link UserDetailsService} para integrar la tabla
 * {@code usuarios} con el sistema de autenticación de Spring Security.
 *
 * <p>Spring Security llama a este servicio durante el proceso de login y
 * también en cada request que lleva un token JWT (a través de
 * {@link com.siglocc.security.JwtAuthFilter}) para cargar los datos del
 * usuario y construir el contexto de seguridad.</p>
 *
 * <p>El método {@code loadUserByUsername} usa el <strong>email</strong>
 * como identificador, ya que ese es el campo de login en este sistema
 * (el parámetro se llama "username" por contrato de la interfaz de Spring).</p>
 *
 * <p>La autoridad se construye con el prefijo {@code ROLE_} seguido del
 * nombre del rol (ej: {@code ROLE_ENL_RECURSOS}), que es el formato
 * estándar que Spring Security usa para evaluar expresiones como
 * {@code hasRole('ENL_RECURSOS')} en los {@code @PreAuthorize}.</p>
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Carga un usuario de la BD por su email y lo convierte al formato
     * que Spring Security entiende.
     *
     * @param email correo electrónico del usuario (parámetro llamado "username" por convención)
     * @return objeto {@link UserDetails} con email, password hasheado, estado activo y autoridades
     * @throws UsernameNotFoundException si no existe un usuario con ese email
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        return new User(
                usuario.getEmail(),
                usuario.getPassword(),
                usuario.isActivo(),   // enabled
                true,                  // accountNonExpired
                true,                  // credentialsNonExpired
                true,                  // accountNonLocked
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getName()))
        );
    }
}
