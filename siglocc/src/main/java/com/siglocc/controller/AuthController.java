package com.siglocc.controller;

import com.siglocc.dto.LoginRequest;
import com.siglocc.dto.LoginResponse;
import com.siglocc.entity.Usuario;
import com.siglocc.repository.UsuarioRepository;
import com.siglocc.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        String token = jwtUtil.generateToken(usuario.getEmail(), usuario.getRol().getName());

        LoginResponse.DetallesEquipo detallesEquipo = new LoginResponse.DetallesEquipo(
                usuario.getEquipo().getId(),
                usuario.getEquipo().getNombre(),
                usuario.getEquipo().getTipo().name(),
                usuario.getEquipo().getEnlId(),
                usuario.getEquipo().getErleId()
        );

        LoginResponse.UsuarioInfo usuarioInfo = new LoginResponse.UsuarioInfo(
                usuario.getId(),
                usuario.getName() + " " + usuario.getLastname(),
                usuario.getEmail(),
                usuario.getRol().getName(),
                detallesEquipo
        );

        return ResponseEntity.ok(new LoginResponse(token, "Bearer", usuarioInfo));
    }
}
