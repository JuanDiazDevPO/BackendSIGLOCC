package com.siglocc.dto;

/**
 * Tipo de ítem logístico (literatura, folletos, guías) para poblar selects del front.
 *
 * @param id             ID a enviar como {@code tipoItemId} en recepciones/asignaciones/entregas
 * @param codigo         código abreviado (OE, FOLLETO, GM, MPG, EMR, LGA, NT)
 * @param nombreCompleto nombre completo descriptivo
 * @param aplicaNinos    {@code true} si el ítem se entrega a niños vía iglesias (Momento 3)
 * @param momento        momento operativo: 1 = Visión, 2 = Capacitación, 3 = Entrega
 */
public record TipoItemResponse(
        Integer id,
        String codigo,
        String nombreCompleto,
        Boolean aplicaNinos,
        Integer momento
) {}
