package com.siglocc.dto;

public record RegistroRequest(
        String name,
        String lastname,
        String email,
        String password,
        Integer roleId,
        Integer equipoId
) {}
