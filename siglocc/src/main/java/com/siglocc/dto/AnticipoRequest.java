package com.siglocc.dto;

import com.siglocc.entity.TipoPresupuesto;
import java.math.BigDecimal;

/**
 * DTO de entrada para crear una solicitud de anticipo.
 *
 * <p>El Front-end solo envía los cuatro campos que el usuario completa
 * manualmente. El sistema resuelve automáticamente los demás datos:</p>
 * <ul>
 *   <li>{@code usuarioId} – se extrae del token JWT de la sesión activa</li>
 *   <li>{@code equipoId}  – se toma del equipo asociado al usuario autenticado</li>
 *   <li>{@code temporadaId} – se consulta la temporada marcada como activa en BD</li>
 * </ul>
 *
 * @param titulo           título corto descriptivo de la solicitud
 * @param descripcion      detalle del gasto que se pretende cubrir
 * @param montoSolicitado  monto en COP que se solicita
 * @param tipoPresupuesto  rubro al que aplica: {@code ENTRENAMIENTO} o {@code MENTOREO}
 */
public record AnticipoRequest(
        String titulo,
        String descripcion,
        BigDecimal montoSolicitado,
        TipoPresupuesto tipoPresupuesto
) {}
