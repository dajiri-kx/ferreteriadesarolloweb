-- ============================================================================
-- FERRETERÍA WEB — SCRIPT DE BASE DE DATOS
-- Basado en: Historias de Usuario G1 (Catálogo y Búsqueda, Carrito y Checkout,
--            Cuenta de Usuario, Administración e Inventario)
-- Motor objetivo: MySQL 8.0+ (usa CHECK, GENERATED COLUMNS, triggers con cursores)
-- ============================================================================
--
-- NOTA IMPORTANTE SOBRE EL DIAGRAMA ER ORIGINAL:
-- El diagrama entregado se usó únicamente como REFERENCIA, no como regla fija,
-- ya que varios criterios de aceptación de las historias de usuario exigen
-- estructuras que el diagrama no contemplaba, y otros criterios (descritos
-- como "muy difíciles") fueron descartados por el equipo y por lo tanto NO
-- se modelan en la base de datos (quedan como responsabilidad de la capa de
-- aplicación). A continuación el detalle de los cambios respecto al ER original:
--
--   AGREGADO respecto al diagrama original:
--   1. PRODUCTO_IMAGEN         -> HU "Ver Ficha Detallada": múltiples imágenes con zoom.
--   2. PRODUCTO_RELACIONADO    -> HU "Ver Ficha Detallada": productos relacionados/complementarios.
--   3. Campos de oferta en PRODUCTO (precio_oferta, oferta_fecha_inicio/fin,
--      columna calculada porcentaje_descuento) -> HU "Ver Productos en Oferta".
--   4. PRODUCTO_DESCUENTO_VOLUMEN -> HU "Configurar Precios y Descuentos"
--      (descuentos automáticos por cantidad comprada).
--   5. METODO_ENVIO + columnas metodo_envio_id/costo_envio en PEDIDO
--      -> HU "Seleccionar Método de Envío" (estándar, express, retiro en tienda).
--      direccion_envio_id se vuelve NULLABLE porque el retiro en tienda no
--      requiere dirección de envío.
--   6. Columnas de seguridad de sesión en USUARIO (intentos_fallidos,
--      fecha_bloqueo, ultima_sesion) -> HU "Iniciar y Cerrar Sesión"
--      (bloqueo tras 5 intentos fallidos, cierre de sesión por inactividad).
--   7. FACTURA -> HU "Gestionar Pedidos de Clientes" (generación de comprobante).
--   8. ALERTA_INVENTARIO -> HU "Controlar Stock de Inventario" (alerta de stock bajo).
--   9. CUPON gana producto_id / categoria_id (nullable) para soportar el
--      campo "aplica_a" (producto, categoría o carrito completo).
--  10. contraseña se guarda como contraseña_hash (nunca en texto plano).
--
--   FUERA DE ALCANCE (criterios descartados por el equipo / no modelables en BD):
--   - Autocompletado en tiempo real y comparador visual de hasta 4 productos:
--     son estado de sesión/UI, no requieren persistencia.
--   - Envío de correos de confirmación, notificaciones push, exportación a
--     Excel/PDF de reportes: responsabilidad de la capa de aplicación
--     (la BD solo expone las vistas con los datos necesarios).
--   - Integración real con pasarela de pago (Stripe/PayPal/SINPE): solo se
--     modela el resultado (referencia_gateway, estado) que la pasarela retorna.
-- ============================================================================

-- ============================================================================
-- 0. BASE DE DATOS Y USUARIO DE APLICACIÓN
-- ============================================================================

-- Crea la base de datos del proyecto (charset/collation acentos en español)
CREATE DATABASE IF NOT EXISTS ferreteria_web
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_spanish_ci;

USE ferreteria_web;

-- Usuario de aplicación con el que el backend se conecta a la BD.
-- IMPORTANTE: cambiar 'CambiarEstaClave123!' por una contraseña real y
-- segura antes de llevar esto a un ambiente que no sea el de desarrollo.
CREATE USER IF NOT EXISTS 'ferreteria_app'@'localhost' IDENTIFIED BY 'CambiarEstaClave123!';

-- Privilegios necesarios para operar la aplicación (CRUD normal).
-- No se otorgan DROP/ALTER/GRANT para limitar el impacto de un usuario
-- comprometido; esos permisos quedan reservados a una cuenta de administrador
-- de BD (DBA) distinta a la cuenta de la aplicación.
GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE
    ON ferreteria_web.* TO 'ferreteria_app'@'localhost';

-- Si el backend se conectará desde otro host (no localhost), crear además:
-- CREATE USER IF NOT EXISTS 'ferreteria_app'@'%' IDENTIFIED BY 'CambiarEstaClave123!';
-- GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE ON ferreteria_web.* TO 'ferreteria_app'@'%';

FLUSH PRIVILEGES;

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 0.1 LIMPIEZA (orden inverso de dependencias)
-- ============================================================================
DROP VIEW IF EXISTS vista_ticket_promedio;
DROP VIEW IF EXISTS vista_productos_mas_vendidos;
DROP VIEW IF EXISTS vista_ventas_mensuales;
DROP VIEW IF EXISTS vista_ventas_semanales;
DROP VIEW IF EXISTS vista_ventas_diarias;
DROP VIEW IF EXISTS vista_cupones_utilizados;
DROP VIEW IF EXISTS vista_stock_bajo;
DROP VIEW IF EXISTS vista_productos_oferta;
DROP VIEW IF EXISTS vista_categoria_conteo;

DROP TABLE IF EXISTS ALERTA_INVENTARIO;
DROP TABLE IF EXISTS FACTURA;
DROP TABLE IF EXISTS PAGO;
DROP TABLE IF EXISTS DETALLE_PEDIDO;
DROP TABLE IF EXISTS PEDIDO;
DROP TABLE IF EXISTS ITEM_CARRITO;
DROP TABLE IF EXISTS CARRITO;
DROP TABLE IF EXISTS CUPON;
DROP TABLE IF EXISTS METODO_ENVIO;
DROP TABLE IF EXISTS FAVORITOS;
DROP TABLE IF EXISTS DIRECCION_ENVIO;
DROP TABLE IF EXISTS USUARIO;
DROP TABLE IF EXISTS PRODUCTO_DESCUENTO_VOLUMEN;
DROP TABLE IF EXISTS PRODUCTO_RELACIONADO;
DROP TABLE IF EXISTS PRODUCTO_IMAGEN;
DROP TABLE IF EXISTS INVENTARIO;
DROP TABLE IF EXISTS PRODUCTO;
DROP TABLE IF EXISTS CATEGORIA;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 1. CATÁLOGO — HU "Buscar por Categoría", "Búsqueda por Nombre o Código",
--    "Ver Ficha Detallada", "Ver Productos en Oferta", "Gestionar Catálogo"
-- ============================================================================

CREATE TABLE CATEGORIA (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL,
    categoria_padre_id  BIGINT NULL,
    CONSTRAINT fk_categoria_padre
        FOREIGN KEY (categoria_padre_id) REFERENCES CATEGORIA(id)
        ON DELETE SET NULL ON UPDATE CASCADE
);
-- Soporta "categorías principales" (categoria_padre_id NULL) y subcategorías.

CREATE TABLE PRODUCTO (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre                VARCHAR(150) NOT NULL,
    sku                   VARCHAR(40)  NOT NULL UNIQUE,
    descripcion           TEXT,
    precio                DECIMAL(10,2) NOT NULL CHECK (precio >= 0),
    categoria_id          BIGINT NOT NULL,
    marca                 VARCHAR(80),
    material               VARCHAR(80),
    dimensiones           VARCHAR(80),
    activo                BOOLEAN NOT NULL DEFAULT TRUE,
    -- Campos de oferta (HU "Ver Productos en Oferta")
    precio_oferta         DECIMAL(10,2) NULL CHECK (precio_oferta IS NULL OR precio_oferta >= 0),
    oferta_fecha_inicio   DATE NULL,
    oferta_fecha_fin      DATE NULL,
    porcentaje_descuento  DECIMAL(5,2) GENERATED ALWAYS AS (
                              CASE
                                  WHEN precio_oferta IS NOT NULL AND precio > 0
                                      THEN ROUND((precio - precio_oferta) / precio * 100, 2)
                                  ELSE NULL
                              END
                          ) STORED,
    fecha_creacion        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_producto_categoria
        FOREIGN KEY (categoria_id) REFERENCES CATEGORIA(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_producto_oferta_precio
        CHECK (precio_oferta IS NULL OR precio_oferta < precio),
    CONSTRAINT chk_producto_oferta_fechas
        CHECK (oferta_fecha_fin IS NULL OR oferta_fecha_inicio IS NULL OR oferta_fecha_fin >= oferta_fecha_inicio)
);

-- Índice de texto completo para "buscar por nombre, SKU y descripción" con
-- ordenamiento por relevancia (HU "Búsqueda por Nombre o Código").
CREATE FULLTEXT INDEX ft_producto_busqueda ON PRODUCTO (nombre, descripcion);
CREATE INDEX idx_producto_categoria ON PRODUCTO (categoria_id);
CREATE INDEX idx_producto_precio ON PRODUCTO (precio);

-- Múltiples imágenes con orden y flag de imagen principal (HU "Ver Ficha Detallada")
CREATE TABLE PRODUCTO_IMAGEN (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id   BIGINT NOT NULL,
    url_imagen    VARCHAR(255) NOT NULL,
    orden         INT NOT NULL DEFAULT 0,
    es_principal  BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_imagen_producto
        FOREIGN KEY (producto_id) REFERENCES PRODUCTO(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- Productos relacionados / complementarios (HU "Ver Ficha Detallada")
CREATE TABLE PRODUCTO_RELACIONADO (
    producto_id             BIGINT NOT NULL,
    producto_relacionado_id BIGINT NOT NULL,
    PRIMARY KEY (producto_id, producto_relacionado_id),
    CONSTRAINT fk_rel_producto
        FOREIGN KEY (producto_id) REFERENCES PRODUCTO(id) ON DELETE CASCADE,
    CONSTRAINT fk_rel_producto_relacionado
        FOREIGN KEY (producto_relacionado_id) REFERENCES PRODUCTO(id) ON DELETE CASCADE,
    CONSTRAINT chk_rel_no_auto_relacion CHECK (producto_id <> producto_relacionado_id)
);

-- Descuentos automáticos por cantidad comprada (HU "Configurar Precios y Descuentos")
CREATE TABLE PRODUCTO_DESCUENTO_VOLUMEN (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id           BIGINT NOT NULL,
    cantidad_minima       INT NOT NULL CHECK (cantidad_minima > 0),
    porcentaje_descuento  DECIMAL(5,2) NOT NULL CHECK (porcentaje_descuento BETWEEN 0 AND 100),
    CONSTRAINT fk_descvol_producto
        FOREIGN KEY (producto_id) REFERENCES PRODUCTO(id) ON DELETE CASCADE
);

-- Disponibilidad en tiempo real + alertas de stock bajo
-- (HU "Ver Ficha Detallada", "Controlar Stock de Inventario")
CREATE TABLE INVENTARIO (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id         BIGINT NOT NULL UNIQUE,
    stock_disponible    INT NOT NULL DEFAULT 0 CHECK (stock_disponible >= 0),
    umbral_minimo       INT NOT NULL DEFAULT 5 CHECK (umbral_minimo >= 0),
    motivo_ajuste       VARCHAR(255),
    fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventario_producto
        FOREIGN KEY (producto_id) REFERENCES PRODUCTO(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- Registro histórico de alertas de stock bajo (HU "Controlar Stock de Inventario")
CREATE TABLE ALERTA_INVENTARIO (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id     BIGINT NOT NULL,
    stock_al_momento INT NOT NULL,
    umbral_minimo   INT NOT NULL,
    fecha_alerta    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atendida        BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_alerta_producto
        FOREIGN KEY (producto_id) REFERENCES PRODUCTO(id) ON DELETE CASCADE
);

-- ============================================================================
-- 2. USUARIOS Y DIRECCIONES — HU "Registrar Cuenta", "Iniciar y Cerrar Sesión",
--    "Gestionar Direcciones de Envío", "Gestionar Lista de Favoritos"
-- ============================================================================

CREATE TABLE USUARIO (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre              VARCHAR(120) NOT NULL,
    correo              VARCHAR(150) NOT NULL UNIQUE,
    contraseña_hash     VARCHAR(255) NOT NULL,
    telefono            VARCHAR(20),
    rol                 VARCHAR(20) NOT NULL DEFAULT 'cliente'
                            CHECK (rol IN ('cliente','administrador','bodega','dueño')),
    estado              VARCHAR(20) NOT NULL DEFAULT 'activo'
                            CHECK (estado IN ('activo','inactivo','bloqueado')),
    fecha_registro      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Seguridad de sesión (HU "Iniciar y Cerrar Sesión")
    intentos_fallidos   INT NOT NULL DEFAULT 0,
    fecha_bloqueo       DATETIME NULL,
    ultima_sesion       DATETIME NULL,
    CONSTRAINT chk_usuario_contrasena_hash CHECK (CHAR_LENGTH(contraseña_hash) >= 8)
);
-- Nota: la regla "mínimo 8 caracteres con número y símbolo" se valida en la
-- capa de aplicación ANTES de generar el hash, ya que sobre el hash
-- (bcrypt/argon2) no se puede validar composición de la contraseña original.

CREATE TABLE DIRECCION_ENVIO (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id          BIGINT NOT NULL,
    alias               VARCHAR(60),
    direccion           VARCHAR(255) NOT NULL,
    codigo_postal       VARCHAR(15) NOT NULL,
    es_predeterminada   BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_direccion_cliente
        FOREIGN KEY (cliente_id) REFERENCES USUARIO(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX idx_direccion_cliente ON DIRECCION_ENVIO (cliente_id);

CREATE TABLE FAVORITOS (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id      BIGINT NOT NULL,
    producto_id     BIGINT NOT NULL,
    fecha_agregado  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_favoritos_cliente
        FOREIGN KEY (cliente_id) REFERENCES USUARIO(id) ON DELETE CASCADE,
    CONSTRAINT fk_favoritos_producto
        FOREIGN KEY (producto_id) REFERENCES PRODUCTO(id) ON DELETE CASCADE,
    CONSTRAINT uq_favorito_unico UNIQUE (cliente_id, producto_id)
);

-- ============================================================================
-- 3. ENVÍO Y CUPONES — HU "Seleccionar Método de Envío", "Aplicar Código de
--    Descuento", "Configurar Precios y Descuentos"
-- ============================================================================

CREATE TABLE METODO_ENVIO (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre                VARCHAR(60) NOT NULL,      -- 'Estándar', 'Express', 'Retiro en tienda'
    descripcion           VARCHAR(255),
    costo_base            DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (costo_base >= 0),
    tiempo_estimado_dias  INT NOT NULL DEFAULT 0,
    requiere_direccion    BOOLEAN NOT NULL DEFAULT TRUE  -- FALSE para retiro en tienda
);

CREATE TABLE CUPON (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo          VARCHAR(40) NOT NULL UNIQUE,
    tipo            VARCHAR(20) NOT NULL CHECK (tipo IN ('porcentaje','monto_fijo')),
    valor           DECIMAL(10,2) NOT NULL CHECK (valor > 0),
    fecha_inicio    DATE NOT NULL,
    fecha_fin       DATE NOT NULL,
    limite_usos     INT NOT NULL CHECK (limite_usos > 0),
    usos_actuales   INT NOT NULL DEFAULT 0 CHECK (usos_actuales >= 0),
    aplica_a        VARCHAR(20) NOT NULL CHECK (aplica_a IN ('producto','categoria','carrito')),
    producto_id     BIGINT NULL,
    categoria_id    BIGINT NULL,
    CONSTRAINT fk_cupon_producto
        FOREIGN KEY (producto_id) REFERENCES PRODUCTO(id) ON DELETE CASCADE,
    CONSTRAINT fk_cupon_categoria
        FOREIGN KEY (categoria_id) REFERENCES CATEGORIA(id) ON DELETE CASCADE,
    CONSTRAINT chk_cupon_fechas CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT chk_cupon_usos CHECK (usos_actuales <= limite_usos),
    -- Coherencia entre aplica_a y las FKs opcionales
    CONSTRAINT chk_cupon_aplica_a CHECK (
        (aplica_a = 'producto'  AND producto_id  IS NOT NULL AND categoria_id IS NULL) OR
        (aplica_a = 'categoria' AND categoria_id IS NOT NULL AND producto_id  IS NULL) OR
        (aplica_a = 'carrito'   AND producto_id  IS NULL     AND categoria_id IS NULL)
    )
);

-- ============================================================================
-- 4. CARRITO — HU "Agregar Productos al Carrito"
-- ============================================================================

CREATE TABLE CARRITO (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id      BIGINT NOT NULL,
    fecha_creacion  DATE NOT NULL DEFAULT (CURRENT_DATE),
    CONSTRAINT fk_carrito_cliente
        FOREIGN KEY (cliente_id) REFERENCES USUARIO(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE ITEM_CARRITO (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrito_id    BIGINT NOT NULL,
    producto_id   BIGINT NOT NULL,
    cantidad      INT NOT NULL CHECK (cantidad > 0),
    CONSTRAINT fk_item_carrito
        FOREIGN KEY (carrito_id) REFERENCES CARRITO(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_producto
        FOREIGN KEY (producto_id) REFERENCES PRODUCTO(id) ON DELETE CASCADE,
    CONSTRAINT uq_item_carrito_producto UNIQUE (carrito_id, producto_id)
);

-- ============================================================================
-- 5. PEDIDOS Y PAGOS — HU "Seleccionar Método de Envío", "Realizar Pago
--    Seguro", "Ver Resumen y Confirmar Pedido", "Gestionar Pedidos de Clientes"
-- ============================================================================

CREATE TABLE PEDIDO (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_orden          VARCHAR(30) NOT NULL UNIQUE,
    cliente_id            BIGINT NOT NULL,
    fecha                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado                VARCHAR(20) NOT NULL DEFAULT 'pendiente'
                              CHECK (estado IN ('pendiente','preparando','enviado','entregado','cancelado')),
    subtotal              DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    costo_envio           DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (costo_envio >= 0),
    descuento_total       DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (descuento_total >= 0),
    total                 DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (total >= 0),
    direccion_envio_id    BIGINT NULL,   -- NULL permitido: retiro en tienda
    metodo_envio_id       BIGINT NOT NULL,
    cupon_id              BIGINT NULL,
    CONSTRAINT fk_pedido_cliente
        FOREIGN KEY (cliente_id) REFERENCES USUARIO(id) ON DELETE RESTRICT,
    CONSTRAINT fk_pedido_direccion
        FOREIGN KEY (direccion_envio_id) REFERENCES DIRECCION_ENVIO(id) ON DELETE RESTRICT,
    CONSTRAINT fk_pedido_metodo_envio
        FOREIGN KEY (metodo_envio_id) REFERENCES METODO_ENVIO(id) ON DELETE RESTRICT,
    CONSTRAINT fk_pedido_cupon
        FOREIGN KEY (cupon_id) REFERENCES CUPON(id) ON DELETE SET NULL
);
CREATE INDEX idx_pedido_cliente ON PEDIDO (cliente_id);
CREATE INDEX idx_pedido_estado_fecha ON PEDIDO (estado, fecha);

CREATE TABLE DETALLE_PEDIDO (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id       BIGINT NOT NULL,
    producto_id     BIGINT NOT NULL,
    cantidad        INT NOT NULL CHECK (cantidad > 0),
    precio_unitario DECIMAL(10,2) NOT NULL CHECK (precio_unitario >= 0),
    CONSTRAINT fk_detalle_pedido
        FOREIGN KEY (pedido_id) REFERENCES PEDIDO(id) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_producto
        FOREIGN KEY (producto_id) REFERENCES PRODUCTO(id) ON DELETE RESTRICT
);

CREATE TABLE PAGO (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id           BIGINT NOT NULL,
    monto               DECIMAL(10,2) NOT NULL CHECK (monto >= 0),
    metodo              VARCHAR(30) NOT NULL CHECK (metodo IN ('tarjeta','transferencia_sinpe','paypal','stripe')),
    estado              VARCHAR(20) NOT NULL DEFAULT 'pendiente'
                            CHECK (estado IN ('pendiente','completado','fallido','reembolsado')),
    referencia_gateway  VARCHAR(120),
    fecha               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pago_pedido
        FOREIGN KEY (pedido_id) REFERENCES PEDIDO(id) ON DELETE CASCADE
);

-- Facturación (HU "Gestionar Pedidos de Clientes": "generar facturas en PDF").
-- La BD guarda el comprobante y la referencia al PDF; la generación del
-- archivo la realiza la capa de aplicación.
CREATE TABLE FACTURA (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id       BIGINT NOT NULL UNIQUE,
    numero_factura  VARCHAR(30) NOT NULL UNIQUE,
    fecha_emision   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    url_pdf         VARCHAR(255),
    CONSTRAINT fk_factura_pedido
        FOREIGN KEY (pedido_id) REFERENCES PEDIDO(id) ON DELETE CASCADE
);

-- ============================================================================
-- 6. TRIGGERS — reglas de negocio derivadas de los criterios de aceptación
-- ============================================================================
DELIMITER $$

-- HU "Controlar Stock de Inventario": "El sistema debe bloquear ventas de
-- productos sin stock disponible."
CREATE TRIGGER trg_detalle_pedido_valida_stock
BEFORE INSERT ON DETALLE_PEDIDO
FOR EACH ROW
BEGIN
    DECLARE stock_actual INT;
    SELECT stock_disponible INTO stock_actual
        FROM INVENTARIO WHERE producto_id = NEW.producto_id;
    IF stock_actual IS NULL OR stock_actual < NEW.cantidad THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Stock insuficiente para completar la venta de este producto.';
    END IF;
END$$

-- HU "Agregar Productos al Carrito": "El sistema debe validar stock
-- disponible al agregar al carrito."
CREATE TRIGGER trg_item_carrito_valida_stock
BEFORE INSERT ON ITEM_CARRITO
FOR EACH ROW
BEGIN
    DECLARE stock_actual INT;
    SELECT stock_disponible INTO stock_actual
        FROM INVENTARIO WHERE producto_id = NEW.producto_id;
    IF stock_actual IS NULL OR stock_actual < NEW.cantidad THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No hay stock suficiente para agregar esa cantidad al carrito.';
    END IF;
END$$

-- HU "Controlar Stock de Inventario": "El sistema debe descontar stock
-- automáticamente al confirmar una venta." Se interpreta "venta confirmada"
-- como el momento en que el PAGO pasa a estado 'completado'.
CREATE TRIGGER trg_pago_descuenta_stock
AFTER UPDATE ON PAGO
FOR EACH ROW
BEGIN
    IF NEW.estado = 'completado' AND OLD.estado <> 'completado' THEN
        UPDATE INVENTARIO inv
        JOIN DETALLE_PEDIDO dp ON dp.producto_id = inv.producto_id
        SET inv.stock_disponible = inv.stock_disponible - dp.cantidad
        WHERE dp.pedido_id = NEW.pedido_id;
    END IF;
END$$

-- HU "Controlar Stock de Inventario": "Se debe enviar alerta cuando el stock
-- baje del umbral configurado." (se registra en ALERTA_INVENTARIO; el envío
-- de la notificación real es responsabilidad de la aplicación).
CREATE TRIGGER trg_inventario_alerta_stock_bajo
AFTER UPDATE ON INVENTARIO
FOR EACH ROW
BEGIN
    IF NEW.stock_disponible <= NEW.umbral_minimo
       AND (OLD.stock_disponible > OLD.umbral_minimo) THEN
        INSERT INTO ALERTA_INVENTARIO (producto_id, stock_al_momento, umbral_minimo)
        VALUES (NEW.producto_id, NEW.stock_disponible, NEW.umbral_minimo);
    END IF;
END$$

-- HU "Iniciar y Cerrar Sesión": "El sistema debe bloquear la cuenta tras 5
-- intentos fallidos."
CREATE TRIGGER trg_usuario_bloqueo_intentos
BEFORE UPDATE ON USUARIO
FOR EACH ROW
BEGIN
    IF NEW.intentos_fallidos >= 5 AND OLD.intentos_fallidos < 5 THEN
        SET NEW.estado = 'bloqueado';
        SET NEW.fecha_bloqueo = NOW();
    END IF;
    -- Un login exitoso reinicia el contador
    IF NEW.ultima_sesion IS NOT NULL AND NEW.ultima_sesion <> OLD.ultima_sesion THEN
        SET NEW.intentos_fallidos = 0;
    END IF;
END$$

-- HU "Aplicar Código de Descuento": valida vigencia y límite de usos del
-- cupón antes de asociarlo a un pedido; "Solo se permite un código de
-- descuento por pedido" queda garantizado porque cupon_id es una única FK.
CREATE TRIGGER trg_pedido_valida_cupon
BEFORE INSERT ON PEDIDO
FOR EACH ROW
BEGIN
    DECLARE v_inicio DATE; DECLARE v_fin DATE;
    DECLARE v_usos INT; DECLARE v_limite INT;
    IF NEW.cupon_id IS NOT NULL THEN
        SELECT fecha_inicio, fecha_fin, usos_actuales, limite_usos
            INTO v_inicio, v_fin, v_usos, v_limite
            FROM CUPON WHERE id = NEW.cupon_id;
        IF CURRENT_DATE NOT BETWEEN v_inicio AND v_fin THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El cupón está expirado o aún no es válido.';
        ELSEIF v_usos >= v_limite THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El cupón alcanzó su límite de usos.';
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_pedido_incrementa_uso_cupon
AFTER INSERT ON PEDIDO
FOR EACH ROW
BEGIN
    IF NEW.cupon_id IS NOT NULL THEN
        UPDATE CUPON SET usos_actuales = usos_actuales + 1 WHERE id = NEW.cupon_id;
    END IF;
END$$

-- HU "Gestionar Direcciones de Envío": solo una dirección predeterminada por
-- cliente ("En el checkout se debe preseleccionar la dirección predeterminada").
CREATE TRIGGER trg_direccion_predeterminada_unica
BEFORE INSERT ON DIRECCION_ENVIO
FOR EACH ROW
BEGIN
    IF NEW.es_predeterminada = TRUE THEN
        UPDATE DIRECCION_ENVIO SET es_predeterminada = FALSE
            WHERE cliente_id = NEW.cliente_id;
    END IF;
END$$

CREATE TRIGGER trg_direccion_predeterminada_unica_upd
BEFORE UPDATE ON DIRECCION_ENVIO
FOR EACH ROW
BEGIN
    IF NEW.es_predeterminada = TRUE AND OLD.es_predeterminada = FALSE THEN
        UPDATE DIRECCION_ENVIO SET es_predeterminada = FALSE
            WHERE cliente_id = NEW.cliente_id AND id <> NEW.id;
    END IF;
END$$

DELIMITER ;

-- ============================================================================
-- 7. VISTAS — soportan directamente los criterios de aceptación de reportes
--    y visualización, sin necesidad de nuevas tablas.
-- ============================================================================

-- HU "Buscar Productos por Categoría": "El sistema debe mostrar el número de
-- productos por categoría."
CREATE VIEW vista_categoria_conteo AS
SELECT c.id AS categoria_id, c.nombre AS categoria,
       COUNT(p.id) AS total_productos
FROM CATEGORIA c
LEFT JOIN PRODUCTO p ON p.categoria_id = c.id AND p.activo = TRUE
GROUP BY c.id, c.nombre;

-- HU "Ver Productos en Oferta": expiran automáticamente según la fecha configurada.
CREATE VIEW vista_productos_oferta AS
SELECT id, nombre, sku, precio AS precio_original, precio_oferta,
       porcentaje_descuento, oferta_fecha_inicio, oferta_fecha_fin
FROM PRODUCTO
WHERE precio_oferta IS NOT NULL
  AND CURRENT_DATE BETWEEN oferta_fecha_inicio AND oferta_fecha_fin
  AND activo = TRUE;

-- HU "Controlar Stock de Inventario": vista de apoyo para el panel de alertas.
CREATE VIEW vista_stock_bajo AS
SELECT p.id AS producto_id, p.nombre, p.sku,
       i.stock_disponible, i.umbral_minimo
FROM INVENTARIO i
JOIN PRODUCTO p ON p.id = i.producto_id
WHERE i.stock_disponible <= i.umbral_minimo;

-- HU "Configurar Precios y Descuentos": "El sistema debe mostrar reporte de
-- cupones utilizados."
CREATE VIEW vista_cupones_utilizados AS
SELECT codigo, tipo, valor, usos_actuales, limite_usos,
       ROUND(usos_actuales / limite_usos * 100, 2) AS porcentaje_uso
FROM CUPON
WHERE usos_actuales > 0;

-- HU "Ver Reportes de Ventas": "ventas totales por día, semana y mes."
CREATE VIEW vista_ventas_diarias AS
SELECT DATE(fecha) AS dia, COUNT(*) AS pedidos, SUM(total) AS ingresos
FROM PEDIDO
WHERE estado <> 'cancelado'
GROUP BY DATE(fecha);

CREATE VIEW vista_ventas_semanales AS
SELECT YEARWEEK(fecha, 3) AS semana, COUNT(*) AS pedidos, SUM(total) AS ingresos
FROM PEDIDO
WHERE estado <> 'cancelado'
GROUP BY YEARWEEK(fecha, 3);

CREATE VIEW vista_ventas_mensuales AS
SELECT DATE_FORMAT(fecha, '%Y-%m') AS mes, COUNT(*) AS pedidos, SUM(total) AS ingresos
FROM PEDIDO
WHERE estado <> 'cancelado'
GROUP BY DATE_FORMAT(fecha, '%Y-%m');

-- HU "Ver Reportes de Ventas": "ranking de productos más vendidos con
-- cantidad e ingresos."
CREATE VIEW vista_productos_mas_vendidos AS
SELECT pr.id AS producto_id, pr.nombre,
       SUM(dp.cantidad) AS unidades_vendidas,
       SUM(dp.cantidad * dp.precio_unitario) AS ingresos_generados
FROM DETALLE_PEDIDO dp
JOIN PEDIDO pe ON pe.id = dp.pedido_id
JOIN PRODUCTO pr ON pr.id = dp.producto_id
WHERE pe.estado <> 'cancelado'
GROUP BY pr.id, pr.nombre
ORDER BY unidades_vendidas DESC;

-- HU "Ver Reportes de Ventas": "el ticket promedio por pedido."
CREATE VIEW vista_ticket_promedio AS
SELECT ROUND(AVG(total), 2) AS ticket_promedio
FROM PEDIDO
WHERE estado <> 'cancelado';

-- ============================================================================
-- 8. DATOS SEMILLA MÍNIMOS (métodos de envío base requeridos por la HU
--    "Seleccionar Método de Envío")
-- ============================================================================
INSERT INTO METODO_ENVIO (nombre, descripcion, costo_base, tiempo_estimado_dias, requiere_direccion) VALUES
('Estándar',        'Entrega a domicilio en tiempo regular', 3000.00, 5, TRUE),
('Express',         'Entrega a domicilio prioritaria',       6000.00, 1, TRUE),
('Retiro en tienda','Retiro sin costo en sucursal',              0.00, 0, FALSE);

-- ============================================================================
-- FIN DEL SCRIPT
-- ============================================================================
