package com.siglocc.dto;

/**
 * Datos necesarios para registrar una iglesia en la operación logística (Momento 1).
 *
 * <p>El {@code equipoId} se toma del JWT; el cliente no debe enviarlo.
 * Todos los campos de contacto del líder son opcionales en el formulario inicial,
 * pero son fundamentales para la coordinación durante la distribución.</p>
 *
 * @param nombre          nombre de la iglesia
 * @param denominacion    denominación o movimiento religioso (opcional)
 * @param departamento    departamento donde se ubica
 * @param ciudad          ciudad donde se ubica
 * @param direccion       dirección detallada (opcional)
 * @param pastorNombre    nombre del pastor (opcional)
 * @param pastorCelular   celular del pastor (opcional)
 * @param pastorCorreo    correo del pastor (opcional)
 * @param nombreLider     nombre del líder de contacto (opcional)
 * @param celularLider    celular del líder (opcional)
 * @param correoLider     correo del líder (opcional)
 * @param temporadaId     ID de la temporada
 * @param cajasSolicitadas número de cajas que la iglesia solicita
 */
public record IglesiaRequest(
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
        Integer temporadaId,
        Integer cajasSolicitadas
) {}
