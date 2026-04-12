package com.siglocc.dto;

import java.util.List;

/**
 * DTO de respuesta para la carga masiva de presupuestos desde archivo CSV.
 *
 * <p>El proceso ETL nunca se detiene ante un error individual: procesa todas las
 * filas del archivo y reporta el resultado final en este DTO. Las filas fallidas
 * incluyen el número de fila y el motivo del error para facilitar la corrección.</p>
 *
 * @param procesados Número total de filas leídas del archivo (excluyendo encabezado)
 * @param exitosos   Número de filas guardadas correctamente en {@code presupuesto_datos}
 * @param fallidos   Número de filas que no pudieron procesarse
 * @param errores    Lista descriptiva de errores con formato "Fila N: motivo del error"
 */
public record CargaMasivaResponse(
        int procesados,
        int exitosos,
        int fallidos,
        List<String> errores
) {
    /**
     * Constructor compacto que hace una copia defensiva de la lista de errores.
     * Esto garantiza que el llamador no pueda mutar el estado interno del DTO
     * después de la construcción.
     */
    public CargaMasivaResponse {
        errores = List.copyOf(errores);
    }
}
