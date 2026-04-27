package com.siglocc.dto;

import com.siglocc.entity.TipoCuenta;
import com.siglocc.entity.TipoPresupuesto;
import java.math.BigDecimal;

/**
 * DTO de entrada para crear una solicitud de anticipo.
 *
 * <p>El Front-end envía los campos del formulario. El sistema resuelve
 * automáticamente los demás datos:</p>
 * <ul>
 *   <li>{@code usuarioId}   – se extrae del token JWT de la sesión activa</li>
 *   <li>{@code equipoId}    – se toma del equipo asociado al usuario autenticado</li>
 *   <li>{@code temporadaId} – se consulta la temporada marcada como activa en BD</li>
 * </ul>
 *
 * @param titulo           título corto descriptivo de la solicitud
 * @param descripcion      detalle del gasto / destino del anticipo
 * @param montoSolicitado  monto en COP que se solicita
 * @param tipoPresupuesto  rubro: {@code ENTRENAMIENTO} o {@code MENTOREO}
 * @param ciudad           ciudad desde donde se hace la solicitud (ej: BUCARAMANGA)
 * @param cedula           cédula del solicitante
 * @param banco            nombre del banco donde se hará el giro (ej: NEQUI, BANCOLOMBIA)
 * @param tipoCuenta       tipo de cuenta destino: AHORROS, CORRIENTE, NEQUI o DAVIPLATA
 * @param numeroCuenta     número de cuenta bancaria del beneficiario
 * @param nombreTitular    nombre completo del titular de la cuenta
 * @param cedulaTitular    cédula del titular de la cuenta bancaria
 */
public record AnticipoRequest(
        String titulo,
        String descripcion,
        BigDecimal montoSolicitado,
        TipoPresupuesto tipoPresupuesto,
        String ciudad,
        String cedula,
        String banco,
        TipoCuenta tipoCuenta,
        String numeroCuenta,
        String nombreTitular,
        String cedulaTitular
) {}
