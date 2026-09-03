package com.siglocc.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta con el detalle completo de una solicitud de anticipo.
 *
 * <p>Se usa en el listado ({@code GET /api/v1/anticipos}), a diferencia de
 * {@link AnticipoResponse} que es la respuesta mínima de crear/aprobar
 * (id, estado, mensaje, rutaPdf). Este trae todos los campos que una
 * pantalla de gestión necesita para mostrar la tabla y el detalle sin
 * hacer una segunda consulta.</p>
 *
 * @param id                    ID de la solicitud
 * @param titulo                título corto de la solicitud
 * @param descripcion           descripción detallada del gasto
 * @param montoSolicitado       monto en COP
 * @param tipoPresupuesto       {@code "ENTRENAMIENTO"} o {@code "MENTOREO"}
 * @param estado                {@code "PENDIENTE"}, {@code "APROBADO"} o {@code "RECHAZADO"}
 * @param motivoRechazo         razón del rechazo automático, o {@code null} si no aplica
 * @param ciudad                ciudad del solicitante
 * @param cedula                cédula del solicitante
 * @param banco                 banco destino del giro
 * @param tipoCuenta            {@code "AHORROS"}, {@code "CORRIENTE"}, {@code "NEQUI"} o {@code "DAVIPLATA"}
 * @param numeroCuenta          número de cuenta del beneficiario
 * @param nombreTitular         nombre del titular de la cuenta
 * @param cedulaTitular         cédula del titular de la cuenta
 * @param rutaPdf               ruta del PDF formal generado, o {@code null} si falló la generación
 * @param equipoId              ID del equipo solicitante
 * @param equipoNombre          nombre del equipo solicitante, o {@code null} si el equipo ya no existe
 * @param temporadaId           ID de la temporada
 * @param usuarioId             ID del usuario que creó la solicitud
 * @param fechaSolicitud        fecha y hora de creación
 * @param fechaAprobacionFinal  fecha y hora de aprobación, o {@code null} si no está aprobada
 */
public record AnticipoDetalleResponse(
        Integer id,
        String titulo,
        String descripcion,
        BigDecimal montoSolicitado,
        String tipoPresupuesto,
        String estado,
        String motivoRechazo,
        String ciudad,
        String cedula,
        String banco,
        String tipoCuenta,
        String numeroCuenta,
        String nombreTitular,
        String cedulaTitular,
        String rutaPdf,
        Integer equipoId,
        String equipoNombre,
        Integer temporadaId,
        Integer usuarioId,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaAprobacionFinal
) {}
