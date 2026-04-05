package com.siglocc.dto;

/**
 * DTO de entrada para el registro de un nuevo usuario.
 *
 * <p>Solo los usuarios con rol {@code ENL_RECURSOS} o {@code ENL_LOGISTICA}
 * pueden invocar el endpoint que usa este DTO.</p>
 *
 * <p>El campo {@code password} se recibe en texto plano y es hasheado con
 * BCrypt en el {@link com.siglocc.service.UsuarioService} antes de persistirse.</p>
 *
 * @param name      nombre de pila del nuevo usuario
 * @param lastname  apellido del nuevo usuario
 * @param email     correo electrónico único (usado como identificador de login)
 * @param password  contraseña en texto plano; se hashea antes de guardar
 * @param roleId    ID del rol a asignar (debe existir en la tabla {@code roles})
 * @param equipoId  ID del equipo al que pertenece (debe existir en {@code equipos})
 */
public record RegistroRequest(
        String name,
        String lastname,
        String email,
        String password,
        Integer roleId,
        Integer equipoId
) {}
