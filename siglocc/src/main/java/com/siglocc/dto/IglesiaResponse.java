package com.siglocc.dto;

import java.time.LocalDateTime;

/**
 * Respuesta con los datos completos de una iglesia registrada.
 *
 * @param id              identificador único
 * @param nombre          nombre de la iglesia
 * @param denominacion    denominación religiosa
 * @param departamento    departamento
 * @param ciudad          ciudad
 * @param direccion       dirección detallada
 * @param pastorNombre    nombre del pastor
 * @param pastorCelular   celular del pastor
 * @param pastorCorreo    correo del pastor
 * @param nombreLider     nombre del líder de contacto
 * @param celularLider    celular del líder
 * @param correoLider     correo del líder
 * @param equipoId        ID del equipo ERL responsable
 * @param temporadaId     ID de la temporada
 * @param cajasSolicitadas número de cajas solicitadas
 * @param estado          estado actual (PENDIENTE, APROBADA, RECHAZADA)
 * @param motivoRechazo   motivo del rechazo (solo si estado = RECHAZADA)
 * @param fechaRegistro   fecha y hora de inscripción
 */
public record IglesiaResponse(
        Integer id,
        String nombre,
        String denominacion,
        String departamento,
        String ciudad,
        String direccion,
        String pastorNombre,
        String pastorCelular,
        String pastorCorreo,
        String nombreLider,
        String celularLider,
        String correoLider,
        Integer equipoId,
        Integer temporadaId,
        Integer cajasSolicitadas,
        String estado,
        String motivoRechazo,
        LocalDateTime fechaRegistro
) {}
