package com.siglocc.dto;

/**
 * DTO de respuesta para las operaciones del módulo de anticipos.
 *
 * <p>Se usa tanto al crear una solicitud como al aprobarla. El campo
 * {@code mensaje} varía según el resultado:</p>
 * <ul>
 *   <li><strong>PENDIENTE:</strong> "Solicitud enviada correctamente y en espera de aprobación del ENL."</li>
 *   <li><strong>RECHAZADO:</strong> "Rechazo automático: El monto solicitado supera el saldo disponible..."</li>
 *   <li><strong>APROBADO:</strong> "Solicitud aprobada exitosamente."</li>
 * </ul>
 *
 * @param id      ID de la solicitud guardada en BD
 * @param estado  estado resultante: {@code PENDIENTE}, {@code RECHAZADO} o {@code APROBADO}
 * @param mensaje descripción legible del resultado para mostrar al usuario
 * @param rutaPdf ruta relativa del PDF generado (ej: {@code anticipos/ANTICIPO_12.pdf});
 *                {@code null} si la generación falló
 */
public record AnticipoResponse(
        Integer id,
        String estado,
        String mensaje,
        String rutaPdf
) {}
