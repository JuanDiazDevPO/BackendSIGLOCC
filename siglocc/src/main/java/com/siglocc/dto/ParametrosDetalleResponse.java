package com.siglocc.dto;

import java.math.BigDecimal;

/**
 * DTO de respuesta con el detalle completo de los parámetros financieros
 * configurados para una temporada.
 *
 * <p>A diferencia de {@link ParametrosResponse} (que solo confirma una operación
 * de guardado), este DTO expone todos los valores para poder pintarlos en pantalla,
 * por ejemplo al consultar el detalle de una temporada.</p>
 *
 * @param id                    ID del registro en {@code parametros_nconnect}.
 * @param temporadaId           ID de la temporada a la que aplican estos parámetros.
 * @param tasaCambio            TRM pactada para la temporada, en COP/USD.
 * @param cajasPorContenedor    estándar de cajas por contenedor.
 * @param porcentajeLga         factor de eficiencia LGA, en decimal (ej: 0.65 = 65%).
 * @param usdAdminCm            costo unitario de administración CM, en USD.
 * @param usdRefrigeroPv        costo de refrigerio por persona en PV, en USD.
 * @param usdTransportePv       costo de transporte por persona en PV, en USD.
 * @param usdTransporteCap      costo de transporte por persona en Capacitación, en USD.
 * @param usdRefrierioCap       costo de refrigerio por persona en Capacitación, en USD.
 * @param visitasMentoreo       número de visitas de mentoría por equipo por temporada.
 * @param personasPorVisita     personas que asisten por visita de mentoría.
 * @param usdTransporteMentoreo costo de transporte por persona en visita de mentoría, en USD.
 * @param usdAlimentoMentoreo   costo de alimentación por persona en visita de mentoría, en USD.
 * @param usdHospedajeMentoreo  costo de hospedaje por persona en visita de mentoría, en USD.
 * @param usdAdminMentoreo      costo de administración por visita de mentoría, en USD.
 */
public record ParametrosDetalleResponse(
        Integer id,
        Integer temporadaId,
        BigDecimal tasaCambio,
        Integer cajasPorContenedor,
        BigDecimal porcentajeLga,
        BigDecimal usdAdminCm,
        BigDecimal usdRefrigeroPv,
        BigDecimal usdTransportePv,
        BigDecimal usdTransporteCap,
        BigDecimal usdRefrierioCap,
        Integer visitasMentoreo,
        Integer personasPorVisita,
        BigDecimal usdTransporteMentoreo,
        BigDecimal usdAlimentoMentoreo,
        BigDecimal usdHospedajeMentoreo,
        BigDecimal usdAdminMentoreo
) {}
