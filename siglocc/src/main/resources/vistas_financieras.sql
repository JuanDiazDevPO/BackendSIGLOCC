-- ============================================================================
-- SIGLOCC — Vistas del motor financiero y de logística
-- ============================================================================
-- Estas 5 vistas nunca estuvieron versionadas en el repo — se crearon a mano
-- directo en la base de datos en algún momento del desarrollo. Este archivo
-- es el resultado de correr SHOW CREATE VIEW sobre cada una en la BD de dev
-- y reformatear el resultado (una sola línea, todo entre backticks) a algo
-- legible.
--
-- ORDEN DE DEPENDENCIA (crear en este orden, cada una lee de la anterior):
--   1. vista_presupuesto_final       — calcula el presupuesto en COP a partir
--                                      de los inputs operativos crudos.
--   2. vista_control_saldos_enl      — le resta lo ya ejecutado (reportes
--                                      APROBADOS) para dar el saldo disponible.
--   3. vista_dashboard_financiero    — le agrega el contexto jerárquico
--                                      (equipo, tipo, erle_id, enl_id).
--   4. vista_metas_calculadas        — independiente; cajas totales/LGA a
--                                      partir de la meta de contenedores.
--   5. vista_inventario_disponible   — independiente; recibido vs. asignado
--                                      por categoría de caja / tipo de ítem.
--
-- Ninguna de las 5 tiene hoy una entidad JPA ni un @Query nativo en el
-- código Java (ver com.siglocc.entity.Vista* para las 2 que sí se consumen
-- desde la app: VistaDashboardFinanciero y VistaControlSaldos). Las otras 3
-- existen solo para consulta manual (Workbench) o para una funcionalidad
-- futura, pero igual se versionan aquí para que no vuelvan a perderse.
--
-- BUG HISTÓRICO CORREGIDO (2026-09-12): la tabla metas_equipo tiene dos
-- columnas — cant_contenedores (huérfana, nunca actualizada por la app,
-- siempre en su default 0) y meta_contenedores (la que el código actual
-- realmente escribe). Las definiciones originales de vista_presupuesto_final
-- y vista_metas_calculadas leían cant_contenedores, así que total_admin_cm
-- y cant_cajas_total/cant_cajas_lga daban 0 para cualquier equipo con datos
-- reales cargados. Las definiciones de abajo ya usan meta_contenedores.
--
-- DEFINER: las 3 originales se crearon con DEFINER=`admin_siglocc`@`%`. Si
-- este script se corre contra otra base de datos donde ese usuario no
-- existe (p. ej. una RDS nueva con solo el usuario admin), hay que quitar
-- la cláusula DEFINER (MySQL la reemplaza por el usuario que ejecuta el
-- CREATE) o cambiarla por el usuario admin correspondiente. Las 2 vistas
-- nuevas se documentan aquí ya sin esa cláusula, por portabilidad.
--
-- CREATE OR REPLACE: seguro de re-correr, no duplica ni rompe nada si la
-- vista ya existe con esta misma definición.
-- ============================================================================


-- ────────────────────────────────────────────────────────────────────────
-- 1. VISTA_PRESUPUESTO_FINAL
-- ────────────────────────────────────────────────────────────────────────
-- Convierte los inputs operativos crudos (presupuesto_datos + metas_equipo)
-- más los costos unitarios en USD (parametros_nconnect) en montos en COP,
-- por equipo y temporada. Es la base de todo el motor financiero.
--
-- Fórmulas (todas multiplican por tasa_cambio al final para pasar a COP):
--   personas_por_pv    = entrenadores_pv + (promedio_cm_cont × 2)
--   total_admin_cm     = meta_contenedores × promedio_cm_cont × usd_admin_cm
--   total_refrigerio_pv = num_pv × personas_por_pv × usd_refrigerio_pv
--   total_transporte_pv = num_pv × personas_por_pv × usd_transporte_pv
--   total_transporte_cap = num_cap_occ × num_entrenadores_cap × usd_transporte_cap
--   total_refrigerio_cap = num_cap_occ × num_entrenadores_cap × usd_refrigerio_cap
--   mentoreo_transporte  = equipos_bajo_mentoreo × visitas_mentoreo × personas_por_visita × usd_transporte_mentoreo
--   mentoreo_alimento    = equipos_bajo_mentoreo × visitas_mentoreo × personas_por_visita × usd_alimento_mentoreo
--   mentoreo_hospedaje   = equipos_bajo_mentoreo × visitas_mentoreo × personas_por_visita × usd_hospedaje_mentoreo
--   mentoreo_admin       = equipos_bajo_mentoreo × personas_por_visita × usd_admin_mentoreo
--       (nota: mentoreo_admin es el único de los 4 que NO multiplica por
--        visitas_mentoreo — así está en producción, se documenta tal cual,
--        no se asume que sea un error sin confirmarlo con negocio)
--
-- Prerequisitos: requiere que el equipo tenga fila en metas_equipo (join
-- interno) y que exista parametros_nconnect para la temporada — coincide
-- con las validaciones que ya hace PresupuestoService antes de dejar
-- ingresar presupuesto_datos.
-- ────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE
    ALGORITHM = UNDEFINED
    DEFINER = `admin_siglocc`@`%`
    SQL SECURITY DEFINER
VIEW `vista_presupuesto_final` AS
SELECT
    `t`.`equipo_id`              AS `equipo_id`,
    `t`.`equipo`                 AS `equipo`,
    `t`.`temporada_id`           AS `temporada_id`,
    `t`.`maestros_lga`           AS `maestros_lga`,
    `t`.`num_pv`                 AS `num_pv`,
    `t`.`num_cap_occ`            AS `num_cap_occ`,
    `t`.`num_entrenadores_cap`   AS `num_entrenadores_cap`,
    `t`.`equipos_bajo_mentoreo`  AS `equipos_bajo_mentoreo`,
    `t`.`total_oracion`          AS `total_oracion`,
    `t`.`tasa_cambio`            AS `tasa_cambio`,
    `t`.`usd_refrigerio_pv`      AS `usd_refrigerio_pv`,
    `t`.`usd_transporte_pv`      AS `usd_transporte_pv`,
    `t`.`usd_transporte_cap`     AS `usd_transporte_cap`,
    `t`.`usd_refrigerio_cap`     AS `usd_refrigerio_cap`,
    `t`.`personas_por_pv`        AS `personas_por_pv`,
    `t`.`total_admin_cm`         AS `total_admin_cm`,
    `t`.`mentoreo_transporte`    AS `mentoreo_transporte`,
    `t`.`mentoreo_alimento`      AS `mentoreo_alimento`,
    `t`.`mentoreo_hospedaje`     AS `mentoreo_hospedaje`,
    `t`.`mentoreo_admin`         AS `mentoreo_admin`,
    ((`t`.`num_pv` * `t`.`personas_por_pv`) * `t`.`usd_refrigerio_pv` * `t`.`tasa_cambio`)
        AS `total_refrigerio_pv`,
    ((`t`.`num_pv` * `t`.`personas_por_pv`) * `t`.`usd_transporte_pv` * `t`.`tasa_cambio`)
        AS `total_transporte_pv`,
    ((`t`.`num_cap_occ` * `t`.`num_entrenadores_cap`) * `t`.`usd_transporte_cap` * `t`.`tasa_cambio`)
        AS `total_transporte_cap`,
    ((`t`.`num_cap_occ` * `t`.`num_entrenadores_cap`) * `t`.`usd_refrigerio_cap` * `t`.`tasa_cambio`)
        AS `total_refrigerio_cap`
FROM (
    SELECT
        `pd`.`equipo_id`                AS `equipo_id`,
        `e`.`nombre`                     AS `equipo`,
        `pd`.`temporada_id`              AS `temporada_id`,
        `pd`.`maestros_lga`              AS `maestros_lga`,
        `pd`.`num_pv`                    AS `num_pv`,
        `pd`.`num_cap_occ`               AS `num_cap_occ`,
        `pd`.`num_entrenadores_cap`      AS `num_entrenadores_cap`,
        `pd`.`equipos_bajo_mentoreo`     AS `equipos_bajo_mentoreo`,
        `pd`.`monto_oracion_cop`         AS `total_oracion`,
        `p`.`tasa_cambio`                AS `tasa_cambio`,
        `p`.`usd_refrigerio_pv`          AS `usd_refrigerio_pv`,
        `p`.`usd_transporte_pv`          AS `usd_transporte_pv`,
        `p`.`usd_transporte_cap`         AS `usd_transporte_cap`,
        `p`.`usd_refrigerio_cap`         AS `usd_refrigerio_cap`,
        (`pd`.`entrenadores_pv` + (`pd`.`promedio_cm_cont` * 2))
            AS `personas_por_pv`,
        ((`m`.`meta_contenedores` * `pd`.`promedio_cm_cont`) * `p`.`usd_admin_cm` * `p`.`tasa_cambio`)
            AS `total_admin_cm`,
        (((`pd`.`equipos_bajo_mentoreo` * `p`.`visitas_mentoreo`) * `p`.`personas_por_visita`) * `p`.`usd_transporte_mentoreo` * `p`.`tasa_cambio`)
            AS `mentoreo_transporte`,
        (((`pd`.`equipos_bajo_mentoreo` * `p`.`visitas_mentoreo`) * `p`.`personas_por_visita`) * `p`.`usd_alimento_mentoreo` * `p`.`tasa_cambio`)
            AS `mentoreo_alimento`,
        (((`pd`.`equipos_bajo_mentoreo` * `p`.`visitas_mentoreo`) * `p`.`personas_por_visita`) * `p`.`usd_hospedaje_mentoreo` * `p`.`tasa_cambio`)
            AS `mentoreo_hospedaje`,
        ((`pd`.`equipos_bajo_mentoreo` * `p`.`personas_por_visita`) * `p`.`usd_admin_mentoreo` * `p`.`tasa_cambio`)
            AS `mentoreo_admin`
    FROM ((`presupuesto_datos` `pd`
        JOIN `equipos` `e` ON (`pd`.`equipo_id` = `e`.`id`))
        JOIN `metas_equipo` `m` ON (`pd`.`equipo_id` = `m`.`equipo_id` AND `pd`.`temporada_id` = `m`.`temporada_id`))
        JOIN `parametros_nconnect` `p` ON (`pd`.`temporada_id` = `p`.`temporada_id`)
) `t`;


-- ────────────────────────────────────────────────────────────────────────
-- 2. VISTA_CONTROL_SALDOS_ENL
-- ────────────────────────────────────────────────────────────────────────
-- Le resta a vista_presupuesto_final lo que ya se ejecutó (reportes
-- mensuales en estado APROBADO) para dar el saldo disponible por equipo.
--
-- presupuesto_entrenamiento = total_admin_cm + total_refrigerio_pv
--                            + total_transporte_pv + total_transporte_cap
--                            + total_refrigerio_cap + total_oracion
-- presupuesto_mentoreo      = mentoreo_transporte + mentoreo_alimento
--                            + mentoreo_hospedaje + mentoreo_admin
--
-- ejecutado_entrenamiento / ejecutado_mentoreo: SUM(reporte_detalles.monto_gastado)
-- de reportes en estado APROBADO, separando por prefijo de categoría
-- (E-% → entrenamiento; M-% u O-% → mentoreo).
--
-- ⚠️ REGLA DE NEGOCIO IMPORTANTE (confirmada, no es un bug):
-- Los anticipos APROBADOS (tabla solicitudes_anticipos) NO aparecen en
-- ningún lado de esta vista — el saldo disponible no se reduce cuando se
-- aprueba un anticipo. El descuento ocurre después, cuando el equipo
-- "legaliza" ese gasto en su reporte mensual (reportes_mensuales +
-- reporte_detalles). Es decir: un anticipo aprobado y aún no legalizado
-- no compromete el saldo mostrado en el dashboard.
-- ────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE
    ALGORITHM = UNDEFINED
    DEFINER = `admin_siglocc`@`%`
    SQL SECURITY DEFINER
VIEW `vista_control_saldos_enl` AS
SELECT
    `v`.`equipo_id`      AS `equipo_id`,
    `v`.`equipo`         AS `equipo`,
    `v`.`temporada_id`   AS `temporada_id`,
    (((((`v`.`total_admin_cm` + `v`.`total_refrigerio_pv`) + `v`.`total_transporte_pv`)
        + `v`.`total_transporte_cap`) + `v`.`total_refrigerio_cap`) + `v`.`total_oracion`)
        AS `presupuesto_entrenamiento`,
    COALESCE((
        SELECT SUM(`rd`.`monto_gastado`)
        FROM (`reporte_detalles` `rd`
            JOIN `reportes_mensuales` `rm` ON (`rd`.`reporte_id` = `rm`.`id`))
        WHERE `rm`.`equipo_id` = `v`.`equipo_id`
          AND `rm`.`temporada_id` = `v`.`temporada_id`
          AND `rm`.`estado` = 'APROBADO'
          AND `rd`.`categoria_codigo` LIKE 'E-%'
    ), 0) AS `ejecutado_entrenamiento`,
    (((`v`.`mentoreo_transporte` + `v`.`mentoreo_alimento`) + `v`.`mentoreo_hospedaje`) + `v`.`mentoreo_admin`)
        AS `presupuesto_mentoreo`,
    COALESCE((
        SELECT SUM(`rd`.`monto_gastado`)
        FROM (`reporte_detalles` `rd`
            JOIN `reportes_mensuales` `rm` ON (`rd`.`reporte_id` = `rm`.`id`))
        WHERE `rm`.`equipo_id` = `v`.`equipo_id`
          AND `rm`.`temporada_id` = `v`.`temporada_id`
          AND `rm`.`estado` = 'APROBADO'
          AND (`rd`.`categoria_codigo` LIKE 'M-%' OR `rd`.`categoria_codigo` LIKE 'O-%')
    ), 0) AS `ejecutado_mentoreo`
FROM `vista_presupuesto_final` `v`;


-- ────────────────────────────────────────────────────────────────────────
-- 3. VISTA_DASHBOARD_FINANCIERO
-- ────────────────────────────────────────────────────────────────────────
-- Le agrega a vista_control_saldos_enl el contexto jerárquico del equipo
-- (nombre, tipo, erle_id, enl_id) y calcula los 3 saldos + gran total.
-- Es la vista que consume directamente DashboardService.
-- ────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE
    ALGORITHM = UNDEFINED
    DEFINER = `admin_siglocc`@`%`
    SQL SECURITY DEFINER
VIEW `vista_dashboard_financiero` AS
SELECT
    `e`.`id`             AS `equipo_id`,
    `e`.`nombre`         AS `equipo_nombre`,
    `e`.`tipo`           AS `equipo_tipo`,
    `e`.`erle_id`        AS `erle_id`,
    `e`.`enl_id`         AS `enl_id`,
    `v`.`temporada_id`   AS `temporada_id`,
    `v`.`presupuesto_entrenamiento` AS `presupuesto_entrenamiento`,
    `v`.`ejecutado_entrenamiento`   AS `ejecutado_entrenamiento`,
    (`v`.`presupuesto_entrenamiento` - `v`.`ejecutado_entrenamiento`)
        AS `saldo_entrenamiento`,
    `v`.`presupuesto_mentoreo`  AS `presupuesto_mentoreo`,
    `v`.`ejecutado_mentoreo`    AS `ejecutado_mentoreo`,
    (`v`.`presupuesto_mentoreo` - `v`.`ejecutado_mentoreo`)
        AS `saldo_mentoreo`,
    (`v`.`presupuesto_entrenamiento` + `v`.`presupuesto_mentoreo`)
        AS `gran_total_presupuesto`,
    (`v`.`ejecutado_entrenamiento` + `v`.`ejecutado_mentoreo`)
        AS `gran_total_ejecutado`,
    ((`v`.`presupuesto_entrenamiento` + `v`.`presupuesto_mentoreo`)
        - (`v`.`ejecutado_entrenamiento` + `v`.`ejecutado_mentoreo`))
        AS `gran_total_saldo`
FROM (`vista_control_saldos_enl` `v`
    JOIN `equipos` `e` ON (`v`.`equipo_id` = `e`.`id`));


-- ────────────────────────────────────────────────────────────────────────
-- 4. VISTA_METAS_CALCULADAS
-- ────────────────────────────────────────────────────────────────────────
-- Traduce la meta de contenedores de un equipo a cantidad de cajas totales
-- y cajas LGA ("Literatura, Ganancia de almas..."), usando el porcentaje y
-- la conversión cajas-por-contenedor de parametros_nconnect. No la consume
-- ningún endpoint hoy; existe para consulta manual.
--
--   cant_cajas_total = meta_contenedores × cajas_por_contenedor
--   cant_cajas_lga   = cant_cajas_total × (porcentaje_lga / 100)
-- ────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE VIEW `vista_metas_calculadas` AS
SELECT
    `m`.`id`                       AS `meta_id`,
    `e`.`nombre`                   AS `equipo`,
    `t`.`nombre`                   AS `temporada`,
    `m`.`meta_contenedores`        AS `cant_contenedores`,
    `p`.`cajas_por_contenedor`     AS `cajas_por_contenedor`,
    `p`.`porcentaje_lga`           AS `porcentaje_lga`,
    (`m`.`meta_contenedores` * `p`.`cajas_por_contenedor`)
        AS `cant_cajas_total`,
    ((`m`.`meta_contenedores` * `p`.`cajas_por_contenedor`) * (`p`.`porcentaje_lga` / 100))
        AS `cant_cajas_lga`
FROM ((`metas_equipo` `m`
    JOIN `parametros_nconnect` `p` ON (`m`.`temporada_id` = `p`.`temporada_id`))
    JOIN `equipos` `e` ON (`m`.`equipo_id` = `e`.`id`))
    JOIN `temporadas` `t` ON (`m`.`temporada_id` = `t`.`id`);


-- ────────────────────────────────────────────────────────────────────────
-- 5. VISTA_INVENTARIO_DISPONIBLE
-- ────────────────────────────────────────────────────────────────────────
-- Por equipo/temporada, compara lo recibido en bodega (recepcion_contenedores
-- + detalle_recepcion_contenedor) contra lo ya asignado en asignaciones
-- CONFIRMADAS (asignacion_cabecera + asignacion_detalle), para dar el
-- disponible real. Es un UNION de dos mitades porque un mismo detalle de
-- recepción/asignación usa categoria_caja_id O tipo_item_id, nunca ambos a
-- la vez (mismo patrón "exactamente uno" que ya usa EntregaService).
-- No la consume ningún endpoint hoy; existe para consulta manual o como
-- base para una futura pantalla de inventario.
-- ────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE VIEW `vista_inventario_disponible` AS
SELECT
    `rc`.`equipo_id`      AS `equipo_id`,
    `rc`.`temporada_id`   AS `temporada_id`,
    `d`.`categoria_caja_id` AS `categoria_caja_id`,
    NULL                  AS `tipo_item_id`,
    SUM(`d`.`cantidad`)   AS `total_recibido`,
    COALESCE((
        SELECT SUM(`ad`.`cantidad_asignada`)
        FROM (`asignacion_detalle` `ad`
            JOIN `asignacion_cabecera` `ac` ON (`ac`.`id` = `ad`.`cabecera_id`))
        WHERE `ac`.`equipo_id` = `rc`.`equipo_id`
          AND `ac`.`temporada_id` = `rc`.`temporada_id`
          AND `ad`.`categoria_caja_id` = `d`.`categoria_caja_id`
          AND `ac`.`estado` = 'CONFIRMADA'
    ), 0) AS `total_asignado`,
    (SUM(`d`.`cantidad`) - COALESCE((
        SELECT SUM(`ad`.`cantidad_asignada`)
        FROM (`asignacion_detalle` `ad`
            JOIN `asignacion_cabecera` `ac` ON (`ac`.`id` = `ad`.`cabecera_id`))
        WHERE `ac`.`equipo_id` = `rc`.`equipo_id`
          AND `ac`.`temporada_id` = `rc`.`temporada_id`
          AND `ad`.`categoria_caja_id` = `d`.`categoria_caja_id`
          AND `ac`.`estado` = 'CONFIRMADA'
    ), 0)) AS `disponible`
FROM (`recepcion_contenedores` `rc`
    JOIN `detalle_recepcion_contenedor` `d` ON (`d`.`recepcion_id` = `rc`.`id`))
WHERE `d`.`categoria_caja_id` IS NOT NULL
GROUP BY `rc`.`equipo_id`, `rc`.`temporada_id`, `d`.`categoria_caja_id`

UNION ALL

SELECT
    `rc`.`equipo_id`      AS `equipo_id`,
    `rc`.`temporada_id`   AS `temporada_id`,
    NULL                  AS `categoria_caja_id`,
    `d`.`tipo_item_id`    AS `tipo_item_id`,
    SUM(`d`.`cantidad`)   AS `total_recibido`,
    COALESCE((
        SELECT SUM(`ad`.`cantidad_asignada`)
        FROM (`asignacion_detalle` `ad`
            JOIN `asignacion_cabecera` `ac` ON (`ac`.`id` = `ad`.`cabecera_id`))
        WHERE `ac`.`equipo_id` = `rc`.`equipo_id`
          AND `ac`.`temporada_id` = `rc`.`temporada_id`
          AND `ad`.`tipo_item_id` = `d`.`tipo_item_id`
          AND `ac`.`estado` = 'CONFIRMADA'
    ), 0) AS `total_asignado`,
    (SUM(`d`.`cantidad`) - COALESCE((
        SELECT SUM(`ad`.`cantidad_asignada`)
        FROM (`asignacion_detalle` `ad`
            JOIN `asignacion_cabecera` `ac` ON (`ac`.`id` = `ad`.`cabecera_id`))
        WHERE `ac`.`equipo_id` = `rc`.`equipo_id`
          AND `ac`.`temporada_id` = `rc`.`temporada_id`
          AND `ad`.`tipo_item_id` = `d`.`tipo_item_id`
          AND `ac`.`estado` = 'CONFIRMADA'
    ), 0)) AS `disponible`
FROM (`recepcion_contenedores` `rc`
    JOIN `detalle_recepcion_contenedor` `d` ON (`d`.`recepcion_id` = `rc`.`id`))
WHERE `d`.`tipo_item_id` IS NOT NULL
GROUP BY `rc`.`equipo_id`, `rc`.`temporada_id`, `d`.`tipo_item_id`;
