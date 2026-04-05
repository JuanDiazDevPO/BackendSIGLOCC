package com.siglocc.dto;

import java.math.BigDecimal;

/**
 * DTO de entrada para la carga manual individual de datos de presupuesto.
 *
 * <p>El front-end debe enviar {@code equipoId} y {@code temporadaId} explícitamente.
 * El servicio valida que:</p>
 * <ol>
 *   <li>Existan parámetros ENL para la temporada indicada (prerequisito).</li>
 *   <li>El equipo tenga meta asignada en {@code metas_equipo} para esa temporada.</li>
 *   <li>El equipo exista en la base de datos.</li>
 * </ol>
 *
 * @param equipoId             ID del equipo que ingresa su presupuesto
 * @param temporadaId          ID de la temporada para la que aplica el presupuesto
 * @param promedioCmCont       Promedio de cajas por Punto de Venta / Contenedor
 * @param numPv                Número de Presentaciones de Visión planificadas
 * @param entrenadoresPv       Entrenadores (staff) por cada PV
 * @param personasPv           Personas invitadas por cada PV
 * @param maestrosLga          Número de maestros LGA (ingreso manual)
 * @param numCapOcc            Número de Capacitaciones OCC en la temporada
 * @param numEntrenadoresCap   Entrenadores por cada Capacitación OCC
 * @param equiposBajoMentoreo  Equipos bajo mentoría directa (multiplicador de logística)
 * @param montoOracionCop      Monto destinado a oración, directo en COP
 */
public record PresupuestoRequest(
        Integer equipoId,
        Integer temporadaId,
        BigDecimal promedioCmCont,
        Integer numPv,
        Integer entrenadoresPv,
        Integer personasPv,
        Integer maestrosLga,
        Integer numCapOcc,
        Integer numEntrenadoresCap,
        Integer equiposBajoMentoreo,
        BigDecimal montoOracionCop
) {}
