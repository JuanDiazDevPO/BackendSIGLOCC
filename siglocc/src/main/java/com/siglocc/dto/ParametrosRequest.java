package com.siglocc.dto;

import java.math.BigDecimal;

/**
 * DTO de entrada para crear o actualizar los parámetros financieros del ENL.
 *
 * <p>Todos los campos son obligatorios. El {@code temporadaId} determina
 * a qué temporada aplican los parámetros. Si ya existe un registro para esa
 * temporada, el servicio lo actualizará; si no, creará uno nuevo.</p>
 *
 * <p>Valores de referencia típicos:</p>
 * <ul>
 *   <li>{@code cajasPorContenedor}: 7368</li>
 *   <li>{@code porcentajeLga}: 0.65</li>
 *   <li>{@code visitasMentoreo}: 3</li>
 *   <li>{@code personasPorVisita}: 2</li>
 * </ul>
 *
 * @param temporadaId          ID de la temporada a configurar
 * @param tasaCambio           TRM pactada en COP/USD
 * @param cajasPorContenedor   Estándar de cajas por contenedor
 * @param porcentajeLga        Factor de eficiencia LGA (0.0 - 1.0)
 * @param usdAdminCm           Costo de administración CM en USD
 * @param usdRefrigeroPv       Costo de refrigerio por persona en PV en USD
 * @param usdTransportePv      Costo de transporte por persona en PV en USD
 * @param usdTransporteCap     Costo de transporte por persona en Capacitación en USD
 * @param usdRefrierioCap      Costo de refrigerio por persona en Capacitación en USD
 * @param visitasMentoreo      Número de visitas de mentoría por equipo (fijo: 3)
 * @param personasPorVisita    Personas por visita de mentoría (fijo: 2)
 * @param usdTransporteMentoreo Costo de transporte por persona en mentoría en USD
 * @param usdAlimentoMentoreo  Costo de alimentación por persona en mentoría en USD
 * @param usdHospedajeMentoreo Costo de hospedaje por persona en mentoría en USD
 * @param usdAdminMentoreo     Costo de administración por visita de mentoría en USD
 */
public record ParametrosRequest(
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
