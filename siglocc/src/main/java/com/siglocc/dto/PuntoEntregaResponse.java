package com.siglocc.dto;

import java.math.BigDecimal;

/**
 * Respuesta con los datos completos de un punto de entrega logística.
 *
 * <p>El campo {@code urlMaps} se genera dinámicamente en el servicio con el formato
 * {@code https://maps.google.com/?q={lat},{lng}} cuando las coordenadas están presentes.
 * Es {@code null} si el punto no tiene coordenadas registradas.</p>
 *
 * @param id                  identificador único
 * @param nombre              nombre del punto
 * @param departamento        departamento
 * @param ciudad              ciudad
 * @param direccion           dirección detallada
 * @param coordenadasLat      latitud GPS
 * @param coordenadasLng      longitud GPS
 * @param urlMaps             enlace de Google Maps generado del GPS (puede ser null)
 * @param restriccionMovilidad condición logística 1
 * @param alturaCuerdas       condición logística 2
 * @param noTejasRotas        condición logística 3
 * @param lugarSeguro         condición logística 4
 * @param facilAcceso         condición logística 5
 * @param equipoId            ID del equipo ERLE que registró el punto
 * @param temporadaId         ID de la temporada
 */
public record PuntoEntregaResponse(
        Integer id,
        String nombre,
        String departamento,
        String ciudad,
        String direccion,
        BigDecimal coordenadasLat,
        BigDecimal coordenadasLng,
        String urlMaps,
        Boolean restriccionMovilidad,
        Boolean alturaCuerdas,
        Boolean noTejasRotas,
        Boolean lugarSeguro,
        Boolean facilAcceso,
        Integer equipoId,
        Integer temporadaId
) {}
