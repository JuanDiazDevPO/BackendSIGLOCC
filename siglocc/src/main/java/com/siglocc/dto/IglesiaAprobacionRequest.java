package com.siglocc.dto;

/**
 * Solicitud para aprobar o rechazar la participación de una iglesia.
 *
 * @param decision      «APROBADA» o «RECHAZADA»
 * @param motivoRechazo obligatorio cuando {@code decision} es «RECHAZADA»
 */
public record IglesiaAprobacionRequest(
        String decision,
        String motivoRechazo
) {}
