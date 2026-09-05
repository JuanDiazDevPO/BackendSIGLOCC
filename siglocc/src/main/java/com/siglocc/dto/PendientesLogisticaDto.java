package com.siglocc.dto;

/**
 * Contadores de acciones pendientes / cuellos de botella accionables del dashboard logístico.
 *
 * @param iglesiasAprobadasSinCapacitar iglesias APROBADAS sin registro de capacitación
 * @param cajasRecibidasSinAsignar      recibidas menos asignadas CONFIRMADAS (nunca negativo)
 * @param asignacionesEnBorrador        corridas de asignación en estado BORRADOR
 * @param entregasSinFirma              actas de entrega sin firma adjunta
 * @param entregasSinFotosNinos         actas sin ninguna foto de entrega a niños (Momento C)
 * @param iglesiasPendientesRevision    iglesias en estado PENDIENTE
 */
public record PendientesLogisticaDto(
        long iglesiasAprobadasSinCapacitar,
        long cajasRecibidasSinAsignar,
        long asignacionesEnBorrador,
        long entregasSinFirma,
        long entregasSinFotosNinos,
        long iglesiasPendientesRevision
) {}
