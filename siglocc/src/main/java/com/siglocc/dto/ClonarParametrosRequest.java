package com.siglocc.dto;

/**
 * DTO de entrada para clonar los parámetros de una temporada a otra.
 *
 * <p>Permite al ENL copiar todos los valores de {@code parametros_nconnect}
 * de una temporada anterior a la nueva, evitando digitar todo desde cero.
 * Solo la {@code tasaCambio} suele necesitar ajuste posterior.</p>
 *
 * @param temporadaOrigenId  ID de la temporada cuyos parámetros se van a copiar
 * @param temporadaDestinoId ID de la nueva temporada que recibirá los parámetros
 */
public record ClonarParametrosRequest(
        Integer temporadaOrigenId,
        Integer temporadaDestinoId
) {}
