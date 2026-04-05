package com.siglocc.dto;

import java.math.BigDecimal;

/**
 * DTO de respuesta que representa una fila del Dashboard financiero consolidado.
 *
 * <p>Cada instancia corresponde a un equipo dentro del rango jerárquico
 * visible para el usuario que hizo la consulta. El Front-end puede usar
 * {@code equipoTipo} para formatear visualmente los distintos niveles
 * (ENL, ERLE, ERL) con diferente estilo o indentación.</p>
 *
 * @param equipoId                ID del equipo
 * @param equipoNombre            Nombre descriptivo del equipo
 * @param equipoTipo              Tipo jerárquico: {@code "ENL"}, {@code "ERLE"} o {@code "ERL"}
 * @param presupuestoEntrenamiento Presupuesto asignado a Entrenamiento en COP
 * @param ejecutadoEntrenamiento  Monto ejecutado en Entrenamiento en COP
 * @param saldoEntrenamiento      Saldo disponible en Entrenamiento en COP
 * @param presupuestoMentoreo     Presupuesto asignado a Mentoría en COP
 * @param ejecutadoMentoreo       Monto ejecutado en Mentoría en COP
 * @param saldoMentoreo           Saldo disponible en Mentoría en COP
 * @param granTotalPresupuesto    Presupuesto total (Entrenamiento + Mentoría) en COP
 * @param granTotalEjecutado      Total ejecutado en COP
 * @param granTotalSaldo          Saldo total disponible en COP
 */
public record DashboardItemResponse(
        Integer equipoId,
        String equipoNombre,
        String equipoTipo,
        BigDecimal presupuestoEntrenamiento,
        BigDecimal ejecutadoEntrenamiento,
        BigDecimal saldoEntrenamiento,
        BigDecimal presupuestoMentoreo,
        BigDecimal ejecutadoMentoreo,
        BigDecimal saldoMentoreo,
        BigDecimal granTotalPresupuesto,
        BigDecimal granTotalEjecutado,
        BigDecimal granTotalSaldo
) {}
