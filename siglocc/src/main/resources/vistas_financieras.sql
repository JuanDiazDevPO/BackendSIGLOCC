-- ============================================================================
-- SIGLOCC — Vistas del motor financiero (presupuesto, saldos, dashboard)
-- ============================================================================
-- Estas 3 vistas nunca estuvieron versionadas en el repo — se crearon a mano
-- directo en la base de datos en algún momento del desarrollo. Este archivo
-- es el resultado de correr SHOW CREATE VIEW sobre cada una en la BD de dev
-- y reformatear el resultado (una sola línea, todo entre backticks) a algo
-- legible. La LÓGICA es una copia exacta — ninguna expresión fue modificada.
--
-- ORDEN DE DEPENDENCIA (crear en este orden, cada una lee de la anterior):
--   1. vista_presupuesto_final     — calcula el presupuesto en COP a partir
--                                    de los inputs operativos crudos.
--   2. vista_control_saldos_enl    — le resta lo ya ejecutado (reportes
--                                    APROBADOS) para dar el saldo disponible.
--   3. vista_dashboard_financiero  — le agrega el contexto jerárquico
--                                    (equipo, tipo, erle_id, enl_id).
--
-- DEFINER: las 3 se crearon con DEFINER=`admin_siglocc`@`%`. Si este script
-- se corre contra otra base de datos donde ese usuario no existe, hay que
-- quitar la cláusula DEFINER (MySQL la reemplaza por el usuario que ejecuta
-- el CREATE) o cambiarla por el usuario admin correspondiente.
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
--   total_admin_cm     = cant_contenedores(meta) × promedio_cm_cont × usd_admin_cm
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
        ((`m`.`cant_contenedores` * `pd`.`promedio_cm_cont`) * `p`.`usd_admin_cm` * `p`.`tasa_cambio`)
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
