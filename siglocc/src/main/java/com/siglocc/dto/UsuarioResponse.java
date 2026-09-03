package com.siglocc.dto;

/**
 * DTO de respuesta tras registrar exitosamente un nuevo usuario.
 *
 * <p>Retorna únicamente la información no sensible del usuario creado.
 * El campo {@code password} nunca se incluye en las respuestas de la API.</p>
 *
 * @param id        ID generado automáticamente por la BD
 * @param name      nombre de pila del usuario
 * @param lastname  apellido del usuario
 * @param email     correo electrónico del usuario
 * @param rol       nombre del rol asignado (ej: "ENL_RECURSOS")
 * @param equipo    nombre del equipo asignado (ej: "Atlántico")
 * @param activo    {@code true} si el usuario puede iniciar sesión
 */
public record UsuarioResponse(
        Integer id,
        String name,
        String lastname,
        String email,
        String rol,
        String equipo,
        boolean activo
) {}
