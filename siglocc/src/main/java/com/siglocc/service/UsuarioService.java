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
        usuario.setPassword(passwordEncoder.encode(request.password()));
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
