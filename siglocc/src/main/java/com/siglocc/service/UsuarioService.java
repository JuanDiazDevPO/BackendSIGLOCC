package com.siglocc.service;

import com.siglocc.dto.RegistroRequest;
import com.siglocc.dto.UsuarioResponse;
import com.siglocc.entity.Equipo;
import com.siglocc.entity.Rol;
import com.siglocc.entity.Usuario;
import com.siglocc.repository.EquipoRepository;
import com.siglocc.repository.RolRepository;
import com.siglocc.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Servicio que contiene la lógica de negocio para la gestión de usuarios.
 *
 * <p>Actualmente gestiona el registro de nuevos usuarios. Solo los roles
 * {@code ENL_RECURSOS} y {@code ENL_LOGISTICA} pueden crear usuarios;
 * esa restricción se aplica en el controlador con {@code @PreAuthorize}.</p>
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EquipoRepository equipoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          EquipoRepository equipoRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.equipoRepository = equipoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * <p>Pasos que ejecuta:</p>
     * <ol>
     *   <li>Verifica que el email no esté ya registrado.</li>
     *   <li>Valida que existan el rol y el equipo indicados en el request.</li>
     *   <li>Hashea la contraseña con BCrypt antes de persistirla.</li>
     *   <li>Guarda el usuario y retorna un DTO con la información no sensible.</li>
     * </ol>
     *
     * @param request datos del nuevo usuario enviados desde el Front-end
     * @return DTO con los datos del usuario creado (sin contraseña)
     * @throws IllegalArgumentException si el email ya existe, o si el rol/equipo no se encuentran
     */
    public UsuarioResponse registrar(RegistroRequest request) {
        if (usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado: " + request.email());
        }

        Rol rol = rolRepository.findById(request.roleId())
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado con id: " + request.roleId()));

        Equipo equipo = equipoRepository.findById(request.equipoId())
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado con id: " + request.equipoId()));

        Usuario usuario = new Usuario();
        usuario.setName(request.name());
        usuario.setLastname(request.lastname());
        usuario.setEmail(request.email());
        usuario.setPassword(passwordEncoder.encode(request.password())); // Nunca se guarda en texto plano
        usuario.setRol(rol);
        usuario.setEquipo(equipo);

        Usuario saved = usuarioRepository.save(usuario);

        return new UsuarioResponse(
                saved.getId(),
                saved.getName(),
                saved.getLastname(),
                saved.getEmail(),
                saved.getRol().getName(),
                saved.getEquipo().getNombre()
        );
    }
}
