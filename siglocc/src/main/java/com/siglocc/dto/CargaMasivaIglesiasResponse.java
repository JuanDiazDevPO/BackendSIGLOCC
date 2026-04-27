package com.siglocc.dto;

import java.util.List;

/**
 * Resultado de la carga masiva de iglesias mediante archivo CSV.
 *
 * <p>El proceso nunca se detiene ante errores individuales: procesa todas las filas
 * y acumula los fallos en {@code errores} con el número de fila y el motivo.</p>
 *
 * @param procesadas número total de filas en el archivo (sin contar encabezado)
 * @param exitosas   filas que generaron un registro exitosamente
 * @param fallidas   filas que fallaron (duplicados, datos inválidos, etc.)
 * @param errores    descripción de cada fallo con el número de fila
 */
public record CargaMasivaIglesiasResponse(
        int procesadas,
        int exitosas,
        int fallidas,
        List<String> errores
) {
    public CargaMasivaIglesiasResponse {
        errores = List.copyOf(errores);
    }
}
