package com.siglocc.dto;

import java.math.BigDecimal;

/**
 * Datos necesarios para registrar un punto de entrega logística.
 *
 * <p>El {@code equipoId} se toma del JWT; el cliente no debe enviarlo.
 * Las coordenadas GPS son opcionales en el registro inicial pero
 * son necesarias para generar el enlace de Google Maps.</p>
 *
 * @param nombre              nombre descriptivo del lugar
 * @param departamento        departamento donde se ubica
 * @param ciudad              ciudad donde se ubica
 * @param direccion           dirección detallada
 * @param coordenadasLat      latitud GPS (opcional)
 * @param coordenadasLng      longitud GPS (opcional)
 * @param restriccionMovilidad ¿tiene restricciones de movilidad para camiones?
 * @param alturaCuerdas       ¿tiene altura adecuada para cuerdas de descargue?
 * @param noTejasRotas        ¿no tiene tejas rotas ni riesgo de filtración?
 * @param lugarSeguro         ¿es seguro para almacenar material?
 * @param facilAcceso         ¿tiene fácil acceso para los equipos?
 * @param temporadaId         ID de la temporada a la que pertenece el punto
 */
public record PuntoEntregaRequest(
        String nombre,
        String departamento,
        String ciudad,
        String direccion,
        BigDecimal coordenadasLat,
        BigDecimal coordenadasLng,
        Boolean restriccionMovilidad,
        Boolean alturaCuerdas,
        Boolean noTejasRotas,
        Boolean lugarSeguro,
        Boolean facilAcceso,
        Integer temporadaId
) {}
