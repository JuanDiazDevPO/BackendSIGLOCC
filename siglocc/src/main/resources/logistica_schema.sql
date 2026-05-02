-- ============================================================
--  MÓDULO DE LOGÍSTICA OCC — Script DDL completo + catálogos
--  Base de datos: MySQL 8+
--  Autor: SIGLOCC Backend
--  Fecha: 2026-04-18
-- ============================================================
--  Ejecutar sobre la misma BD donde está el resto del esquema.
--  Todas las tablas usan ENGINE=InnoDB y charset utf8mb4.
--  El script es IDEMPOTENTE: puede ejecutarse varias veces
--  sin error gracias a IF NOT EXISTS y ON DUPLICATE KEY UPDATE.
-- ============================================================

-- ============================================================
-- 1. CATÁLOGO DE ÍTEMS LOGÍSTICOS (literatura y entregables)
--    OE ya NO está aquí — las cajas se gestionan por categoría
--    de género + edad en la tabla categorias_caja.
-- ============================================================
CREATE TABLE IF NOT EXISTS tipos_item (
    id              INT            NOT NULL AUTO_INCREMENT,
    codigo          VARCHAR(10)    NOT NULL,
    nombre_completo VARCHAR(100)   NOT NULL,
    momento         TINYINT        NOT NULL COMMENT '1=Visión, 2=Capacitación, 3=Entrega',
    PRIMARY KEY (id),
    CONSTRAINT uq_tipo_item_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Catálogo de literatura
-- Momento 1: FOLLETO → a todos los pastores en la Visión
-- Momento 2: GM, MPG → a cada maestro capacitado
-- Momento 3: EMR, LGA, NT → acompañan las cajas a las iglesias
INSERT INTO tipos_item (codigo, nombre_completo, momento)
VALUES
    ('FOLLETO', 'Folleto de Visión para pastores',              1),
    ('GM',      'Guía Ministerial para maestros',               2),
    ('MPG',     'Libro Presentación del Evangelio (maestros)',  2),
    ('EMR',     'Cartilla El Mejor Regalo (niños)',             3),
    ('LGA',     'Literatura LGA (niños)',                       3),
    ('NT',      'Nuevo Testamento (niños)',                     3)
ON DUPLICATE KEY UPDATE
    nombre_completo = VALUES(nombre_completo),
    momento         = VALUES(momento);


-- ============================================================
-- 2. CATEGORÍAS DE CAJAS OCC (rangos estándar Samaritan's Purse)
--    6 categorías: género (NINO/NINA) × edad (2-4, 5-9, 10-14)
--    Estas son las unidades del inventario de cajas.
-- ============================================================
CREATE TABLE IF NOT EXISTS categorias_caja (
    id          INT         NOT NULL AUTO_INCREMENT,
    codigo      VARCHAR(20) NOT NULL
                COMMENT 'Ej: NINO_2_4, NINA_5_9',
    genero      ENUM('NINO','NINA') NOT NULL,
    edad_min    TINYINT     NOT NULL,
    edad_max    TINYINT     NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_categoria_caja_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6 categorías estándar SP
INSERT INTO categorias_caja (codigo, genero, edad_min, edad_max, descripcion)
VALUES
    ('NINO_2_4',   'NINO', 2,  4,  'Niño 2-4 años'),
    ('NINO_5_9',   'NINO', 5,  9,  'Niño 5-9 años'),
    ('NINO_10_14', 'NINO', 10, 14, 'Niño 10-14 años'),
    ('NINA_2_4',   'NINA', 2,  4,  'Niña 2-4 años'),
    ('NINA_5_9',   'NINA', 5,  9,  'Niña 5-9 años'),
    ('NINA_10_14', 'NINA', 10, 14, 'Niña 10-14 años')
ON DUPLICATE KEY UPDATE
    descripcion = VALUES(descripcion);


-- ============================================================
-- 3. PUNTOS DE ENTREGA
--    Solo equipos ERLE registran. Checklist de 5 condiciones
--    logísticas + coordenadas GPS para enlace Google Maps.
-- ============================================================
CREATE TABLE IF NOT EXISTS puntos_entrega (
    id                    INT             NOT NULL AUTO_INCREMENT,
    nombre                VARCHAR(100)    NOT NULL,
    departamento          VARCHAR(50)     NOT NULL,
    ciudad                VARCHAR(50)     NOT NULL,
    direccion             TEXT,
    coordenadas_lat       DECIMAL(9,6),
    coordenadas_lng       DECIMAL(9,6),
    -- Checklist logístico del lugar
    restriccion_movilidad TINYINT(1) NOT NULL DEFAULT 0
                          COMMENT '¿Restricción de movilidad para camiones?',
    altura_cuerdas        TINYINT(1) NOT NULL DEFAULT 0
                          COMMENT '¿Altura adecuada para cuerdas de descargue?',
    no_tejas_rotas        TINYINT(1) NOT NULL DEFAULT 0
                          COMMENT '¿Sin tejas rotas ni riesgo de filtración?',
    lugar_seguro          TINYINT(1) NOT NULL DEFAULT 0
                          COMMENT '¿Seguro para almacenar material?',
    facil_acceso          TINYINT(1) NOT NULL DEFAULT 0
                          COMMENT '¿Fácil acceso para equipos de distribución?',
    equipo_id             INT         NOT NULL,
    temporada_id          INT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_punto_equipo    FOREIGN KEY (equipo_id)    REFERENCES equipos(id),
    CONSTRAINT fk_punto_temporada FOREIGN KEY (temporada_id) REFERENCES temporadas(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 4. IGLESIAS (Momento 1 – La Visión)
--    Una iglesia se reinscribe por temporada. Solo pide un
--    número total de cajas; el desglose por categoría lo
--    calcula el motor de asignación inteligente.
-- ============================================================
CREATE TABLE IF NOT EXISTS iglesias (
    id                INT          NOT NULL AUTO_INCREMENT,
    nombre            VARCHAR(100) NOT NULL,
    denominacion      VARCHAR(100),
    departamento      VARCHAR(50)  NOT NULL,
    ciudad            VARCHAR(50)  NOT NULL,
    direccion         TEXT,
    pastor_nombre     VARCHAR(100),
    pastor_celular    VARCHAR(20),
    pastor_correo     VARCHAR(100),
    nombre_lider      VARCHAR(100),
    celular_lider     VARCHAR(20),
    correo_lider      VARCHAR(100),
    equipo_id         INT          NOT NULL,
    temporada_id      INT          NOT NULL,
    cajas_solicitadas INT          COMMENT 'Total de cajas pedidas (sin desglose por categoría)',
    estado            ENUM('PENDIENTE','APROBADA','RECHAZADA') NOT NULL DEFAULT 'PENDIENTE',
    motivo_rechazo    TEXT,
    fecha_registro    DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uq_iglesia_nombre_equipo_temporada
        UNIQUE (nombre, equipo_id, temporada_id),
    CONSTRAINT fk_iglesia_equipo    FOREIGN KEY (equipo_id)    REFERENCES equipos(id),
    CONSTRAINT fk_iglesia_temporada FOREIGN KEY (temporada_id) REFERENCES temporadas(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 5. RECEPCIÓN DE CONTENEDORES (cabecera)
--    Solo registra el total de cajas recibidas. El desglose
--    por categoría y literatura va en detalle_recepcion_contenedor.
-- ============================================================
CREATE TABLE IF NOT EXISTS recepcion_contenedores (
    id                      INT          NOT NULL AUTO_INCREMENT,
    numero_contenedor       VARCHAR(50)  NOT NULL,
    punto_entrega_id        INT          NOT NULL,
    temporada_id            INT          NOT NULL,
    fecha_llegada           DATE         NOT NULL,
    total_cajas_recibidas   INT          NOT NULL
                            COMMENT 'Suma de todas las categorías de caja — calculado automáticamente',
    equipo_id               INT          NOT NULL,
    observaciones           TEXT,
    lista_transportadora_url VARCHAR(500),
    documento_abc_url        VARCHAR(500),
    fecha_registro          DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_rec_punto    FOREIGN KEY (punto_entrega_id) REFERENCES puntos_entrega(id),
    CONSTRAINT fk_rec_temporada FOREIGN KEY (temporada_id)   REFERENCES temporadas(id),
    CONSTRAINT fk_rec_equipo   FOREIGN KEY (equipo_id)       REFERENCES equipos(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 6. DETALLE DE RECEPCIÓN DE CONTENEDOR
--    Una fila por cada categoría de caja o tipo de literatura
--    que llegó en el contenedor.
--    REGLA: exactamente uno de (categoria_caja_id, tipo_item_id)
--    debe ser no nulo por fila.
-- ============================================================
CREATE TABLE IF NOT EXISTS detalle_recepcion_contenedor (
    id                INT NOT NULL AUTO_INCREMENT,
    recepcion_id      INT NOT NULL,
    categoria_caja_id INT NULL COMMENT 'Para cajas OCC (NINO_2_4, NINA_5_9, etc.)',
    tipo_item_id      INT NULL COMMENT 'Para literatura (FOLLETO, GM, EMR, LGA, NT)',
    cantidad          INT NOT NULL CHECK (cantidad > 0),
    PRIMARY KEY (id),
    CONSTRAINT fk_det_rec_recepcion    FOREIGN KEY (recepcion_id)      REFERENCES recepcion_contenedores(id)
                                       ON DELETE CASCADE,
    CONSTRAINT fk_det_rec_categoria    FOREIGN KEY (categoria_caja_id) REFERENCES categorias_caja(id),
    CONSTRAINT fk_det_rec_tipo_item    FOREIGN KEY (tipo_item_id)      REFERENCES tipos_item(id),
    -- Solo uno de los dos debe estar presente
    CONSTRAINT chk_det_rec_exactamente_uno
        CHECK ((categoria_caja_id IS NOT NULL) <> (tipo_item_id IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 7. FOTOS DE CONTENEDOR (Momento A)
--    Máx. 4 fotos. La foto orden=1 muestra el número del contenedor.
-- ============================================================
CREATE TABLE IF NOT EXISTS fotos_contenedor (
    id           INT          NOT NULL AUTO_INCREMENT,
    recepcion_id INT          NOT NULL,
    url          VARCHAR(500) NOT NULL,
    orden        TINYINT      NOT NULL,
    descripcion  VARCHAR(200),
    subido_por   INT          NOT NULL,
    fecha_carga  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_foto_cont_rec     FOREIGN KEY (recepcion_id) REFERENCES recepcion_contenedores(id)
                                    ON DELETE CASCADE,
    CONSTRAINT fk_foto_cont_usuario FOREIGN KEY (subido_por)   REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 8. ASIGNACIÓN INTELIGENTE — CABECERA
--    Un registro por corrida de asignación por equipo+temporada.
--    Estado BORRADOR: generada, pendiente de confirmación.
--    Estado CONFIRMADA: el coordinador aprobó; descarga inventario.
-- ============================================================
CREATE TABLE IF NOT EXISTS asignacion_cabecera (
    id                       INT             NOT NULL AUTO_INCREMENT,
    equipo_id                INT             NOT NULL,
    temporada_id             INT             NOT NULL,
    fecha_generacion         DATETIME        NOT NULL,
    generada_automaticamente TINYINT(1)      NOT NULL DEFAULT 1,
    estado                   ENUM('BORRADOR','CONFIRMADA') NOT NULL DEFAULT 'BORRADOR',
    total_cajas_disponibles  INT
                             COMMENT 'Snapshot: total de cajas al momento de generar',
    total_cajas_solicitadas  INT
                             COMMENT 'Snapshot: sum(cajas_solicitadas) de iglesias aprobadas',
    factor_reduccion         DECIMAL(8,4)
                             COMMENT '< 1.0 cuando la demanda supera el stock disponible',
    observaciones            TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_asig_cab_equipo    FOREIGN KEY (equipo_id)    REFERENCES equipos(id),
    CONSTRAINT fk_asig_cab_temporada FOREIGN KEY (temporada_id) REFERENCES temporadas(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 9. ASIGNACIÓN INTELIGENTE — DETALLE
--    Una fila por iglesia × categoría de caja o ítem de literatura.
--    El campo ajustada_manualmente queda en TRUE si el coordinador
--    modificó la cantidad generada automáticamente.
-- ============================================================
CREATE TABLE IF NOT EXISTS asignacion_detalle (
    id                   INT NOT NULL AUTO_INCREMENT,
    cabecera_id          INT NOT NULL,
    iglesia_id           INT NOT NULL,
    categoria_caja_id    INT NULL COMMENT 'Categoría de caja asignada (nulo si es literatura)',
    tipo_item_id         INT NULL COMMENT 'Tipo de literatura asignada (nulo si es caja)',
    cantidad_asignada    INT NOT NULL CHECK (cantidad_asignada >= 0),
    ajustada_manualmente TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_asig_det_cabecera   FOREIGN KEY (cabecera_id)       REFERENCES asignacion_cabecera(id)
                                      ON DELETE CASCADE,
    CONSTRAINT fk_asig_det_iglesia    FOREIGN KEY (iglesia_id)        REFERENCES iglesias(id),
    CONSTRAINT fk_asig_det_categoria  FOREIGN KEY (categoria_caja_id) REFERENCES categorias_caja(id),
    CONSTRAINT fk_asig_det_tipo_item  FOREIGN KEY (tipo_item_id)      REFERENCES tipos_item(id),
    -- Solo uno de los dos debe estar presente
    CONSTRAINT chk_asig_det_exactamente_uno
        CHECK ((categoria_caja_id IS NOT NULL) <> (tipo_item_id IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 10. CAPACITACIONES DE IGLESIA (Momento 2 – La Capacitación)
--     cajas_calculadas = maestros_enviados × 25
-- ============================================================
CREATE TABLE IF NOT EXISTS capacitaciones_iglesia (
    id                 INT  NOT NULL AUTO_INCREMENT,
    iglesia_id         INT  NOT NULL,
    temporada_id       INT  NOT NULL,
    fecha_capacitacion DATE,
    maestros_enviados  INT  NOT NULL,
    cajas_calculadas   INT  NOT NULL COMMENT 'maestros_enviados × 25',
    gm_entregados      INT  NOT NULL COMMENT 'Guías Ministeriales entregadas',
    mpg_entregados     INT  NOT NULL COMMENT 'Libros MPG entregados',
    observaciones      TEXT,
    PRIMARY KEY (id),
    CONSTRAINT uq_cap_iglesia_temporada UNIQUE (iglesia_id, temporada_id),
    CONSTRAINT fk_cap_iglesia   FOREIGN KEY (iglesia_id)   REFERENCES iglesias(id),
    CONSTRAINT fk_cap_temporada FOREIGN KEY (temporada_id) REFERENCES temporadas(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 11. ENTREGAS A IGLESIAS (Momento 3 – La Entrega)
-- ============================================================
CREATE TABLE IF NOT EXISTS entregas_iglesia (
    id               INT         NOT NULL AUTO_INCREMENT,
    iglesia_id       INT         NOT NULL,
    punto_entrega_id INT         NOT NULL,
    temporada_id     INT         NOT NULL,
    equipo_id        INT         NOT NULL,
    fecha_entrega    DATE,
    firma_tipo       ENUM('DIGITAL','ESCANEADA'),
    firma_url        VARCHAR(500),
    estado           ENUM('PENDIENTE','COMPLETADA','PARCIAL') NOT NULL DEFAULT 'PENDIENTE',
    observaciones    TEXT,
    confirmado       TINYINT(1)  NOT NULL DEFAULT 0,
    fecha_registro   DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uq_entrega_iglesia_temporada UNIQUE (iglesia_id, temporada_id),
    CONSTRAINT fk_ent_iglesia   FOREIGN KEY (iglesia_id)       REFERENCES iglesias(id),
    CONSTRAINT fk_ent_punto     FOREIGN KEY (punto_entrega_id) REFERENCES puntos_entrega(id),
    CONSTRAINT fk_ent_temporada FOREIGN KEY (temporada_id)     REFERENCES temporadas(id),
    CONSTRAINT fk_ent_equipo    FOREIGN KEY (equipo_id)        REFERENCES equipos(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 12. DETALLE DE ENTREGA A IGLESIA
--     Una fila por categoría de caja o tipo de literatura entregado.
-- ============================================================
CREATE TABLE IF NOT EXISTS detalle_entrega_iglesia (
    id                   INT NOT NULL AUTO_INCREMENT,
    entrega_id           INT NOT NULL,
    categoria_caja_id    INT NULL COMMENT 'Para cajas OCC entregadas',
    tipo_item_id         INT NULL COMMENT 'Para literatura entregada',
    cantidad_entregada   INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_det_ent_entrega    FOREIGN KEY (entrega_id)        REFERENCES entregas_iglesia(id)
                                     ON DELETE CASCADE,
    CONSTRAINT fk_det_ent_categoria  FOREIGN KEY (categoria_caja_id) REFERENCES categorias_caja(id),
    CONSTRAINT fk_det_ent_tipo_item  FOREIGN KEY (tipo_item_id)      REFERENCES tipos_item(id),
    CONSTRAINT chk_det_ent_exactamente_uno
        CHECK ((categoria_caja_id IS NOT NULL) <> (tipo_item_id IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 13. FOTOS ENTREGA A IGLESIA (Momento B)
-- ============================================================
CREATE TABLE IF NOT EXISTS fotos_entrega_iglesia (
    id          INT          NOT NULL AUTO_INCREMENT,
    entrega_id  INT          NOT NULL,
    url         VARCHAR(500) NOT NULL,
    orden       INT          NOT NULL,
    subido_por  INT          NOT NULL,
    fecha_carga DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_foto_ent_entrega  FOREIGN KEY (entrega_id) REFERENCES entregas_iglesia(id)
                                    ON DELETE CASCADE,
    CONSTRAINT fk_foto_ent_usuario  FOREIGN KEY (subido_por) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 14. FOTOS ENTREGA A NIÑOS (Momento C)
-- ============================================================
CREATE TABLE IF NOT EXISTS fotos_entrega_ninos (
    id           INT          NOT NULL AUTO_INCREMENT,
    iglesia_id   INT          NOT NULL,
    temporada_id INT          NOT NULL,
    url          VARCHAR(500) NOT NULL,
    orden        INT          NOT NULL,
    subido_por   INT          NOT NULL,
    fecha_carga  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_foto_nin_iglesia   FOREIGN KEY (iglesia_id)   REFERENCES iglesias(id),
    CONSTRAINT fk_foto_nin_temporada FOREIGN KEY (temporada_id) REFERENCES temporadas(id),
    CONSTRAINT fk_foto_nin_usuario   FOREIGN KEY (subido_por)   REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- VISTA: INVENTARIO DISPONIBLE POR EQUIPO Y TEMPORADA
--
-- Lógica:
--   total_recibido  = suma de todo lo que llegó en contenedores
--   total_asignado  = suma de asignaciones CONFIRMADAS
--   disponible      = total_recibido - total_asignado
--
-- Las asignaciones en BORRADOR no descuentan el inventario;
-- solo lo hacen al CONFIRMAR.
-- ============================================================
CREATE OR REPLACE VIEW vista_inventario_disponible AS

-- Cajas por categoría
SELECT
    rc.equipo_id,
    rc.temporada_id,
    d.categoria_caja_id,
    NULL                           AS tipo_item_id,
    SUM(d.cantidad)                AS total_recibido,
    COALESCE(
        (SELECT SUM(ad.cantidad_asignada)
         FROM   asignacion_detalle  ad
         JOIN   asignacion_cabecera ac ON ac.id = ad.cabecera_id
         WHERE  ac.equipo_id         = rc.equipo_id
           AND  ac.temporada_id      = rc.temporada_id
           AND  ad.categoria_caja_id = d.categoria_caja_id
           AND  ac.estado            = 'CONFIRMADA'), 0)
                                   AS total_asignado,
    SUM(d.cantidad) - COALESCE(
        (SELECT SUM(ad.cantidad_asignada)
         FROM   asignacion_detalle  ad
         JOIN   asignacion_cabecera ac ON ac.id = ad.cabecera_id
         WHERE  ac.equipo_id         = rc.equipo_id
           AND  ac.temporada_id      = rc.temporada_id
           AND  ad.categoria_caja_id = d.categoria_caja_id
           AND  ac.estado            = 'CONFIRMADA'), 0)
                                   AS disponible
FROM  recepcion_contenedores        rc
JOIN  detalle_recepcion_contenedor  d  ON d.recepcion_id = rc.id
WHERE d.categoria_caja_id IS NOT NULL
GROUP BY rc.equipo_id, rc.temporada_id, d.categoria_caja_id

UNION ALL

-- Literatura por tipo de ítem
SELECT
    rc.equipo_id,
    rc.temporada_id,
    NULL                           AS categoria_caja_id,
    d.tipo_item_id,
    SUM(d.cantidad)                AS total_recibido,
    COALESCE(
        (SELECT SUM(ad.cantidad_asignada)
         FROM   asignacion_detalle  ad
         JOIN   asignacion_cabecera ac ON ac.id = ad.cabecera_id
         WHERE  ac.equipo_id    = rc.equipo_id
           AND  ac.temporada_id = rc.temporada_id
           AND  ad.tipo_item_id = d.tipo_item_id
           AND  ac.estado       = 'CONFIRMADA'), 0)
                                   AS total_asignado,
    SUM(d.cantidad) - COALESCE(
        (SELECT SUM(ad.cantidad_asignada)
         FROM   asignacion_detalle  ad
         JOIN   asignacion_cabecera ac ON ac.id = ad.cabecera_id
         WHERE  ac.equipo_id    = rc.equipo_id
           AND  ac.temporada_id = rc.temporada_id
           AND  ad.tipo_item_id = d.tipo_item_id
           AND  ac.estado       = 'CONFIRMADA'), 0)
                                   AS disponible
FROM  recepcion_contenedores        rc
JOIN  detalle_recepcion_contenedor  d  ON d.recepcion_id = rc.id
WHERE d.tipo_item_id IS NOT NULL
GROUP BY rc.equipo_id, rc.temporada_id, d.tipo_item_id;


-- ============================================================
-- VERIFICACIÓN FINAL
-- ============================================================
SELECT
    table_name  AS `Tabla`,
    table_rows  AS `Filas aprox.`,
    create_time AS `Creada`
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
      'tipos_item', 'categorias_caja',
      'puntos_entrega', 'iglesias',
      'recepcion_contenedores', 'detalle_recepcion_contenedor',
      'fotos_contenedor',
      'asignacion_cabecera', 'asignacion_detalle',
      'capacitaciones_iglesia',
      'entregas_iglesia', 'detalle_entrega_iglesia',
      'fotos_entrega_iglesia', 'fotos_entrega_ninos'
  )
ORDER BY create_time;
