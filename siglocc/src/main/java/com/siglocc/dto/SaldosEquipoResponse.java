package com.siglocc.dto;

import java.math.BigDecimal;

/**
 * DTO de respuesta para el endpoint {@code GET /api/v1/anticipos/mis-saldos}.
 *
 * <p>Devuelve el estado presupuestal del equipo del usuario autenticado en la
 * temporada activa, desglosado por rubro (ENTRENAMIENTO y MENTOREO).</p>
 *
 * @param equipoId      ID del equipo al que pertenece el usuario
 * @param equipoNombre  nombre descriptivo del equipo
 * @param temporadaId   ID de la temporada activa consultada
 * @param entrenamiento saldos del rubro de entrenamiento
 * @param mentoreo      saldos del rubro de mentoreo
 */
public record SaldosEquipoResponse(
        Integer equipoId,
        String equipoNombre,
        Integer temporadaId,
        RubroSaldo entrenamiento,
        RubroSaldo mentoreo
) {

    /**
     * Detalle financiero de un rubro presupuestal.
     *
     * @param presupuesto monto total asignado al rubro en COP
     * @param ejecutado   monto ya consumido (anticipos aprobados) en COP
     * @param disponible  saldo disponible ({@code presupuesto - ejecutado}) en COP
     */
    public record RubroSaldo(
            BigDecimal presupuesto,
            BigDecimal ejecutado,
            BigDecimal disponible
    ) {}
}
