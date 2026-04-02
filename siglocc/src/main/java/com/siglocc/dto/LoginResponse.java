package com.siglocc.dto;

public record LoginResponse(
        String token,
        String type,
        UsuarioInfo usuario
) {
    public record UsuarioInfo(
            Integer id,
            String nombreCompleto,
            String email,
            String rol,
            DetallesEquipo detallesEquipo
    ) {}

    public record DetallesEquipo(
            Integer id,
            String nombre,
            String tipo,
            Integer enlId,
            Integer erleId
    ) {}
}
