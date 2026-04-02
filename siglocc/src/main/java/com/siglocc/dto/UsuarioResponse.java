package com.siglocc.dto;

public record UsuarioResponse(
        Integer id,
        String name,
        String lastname,
        String email,
        String rol,
        String equipo
) {}
