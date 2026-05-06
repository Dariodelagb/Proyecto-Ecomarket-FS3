-- ============================================================
-- SCRIPT DE INICIALIZACIÓN - ECOMARKET
-- Se ejecuta automáticamente al iniciar el contenedor MySQL
-- ============================================================

USE sistema_ventas;

-- ============================================================
-- Creacion de tablas
-- ============================================================

-- Desactivar llaves foráneas para evitar conflictos de orden al crear
SET FOREIGN_KEY_CHECKS = 0;

-- 1. Tabla de Categorías
CREATE TABLE IF NOT EXISTS CategoriaProducto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    categoria VARCHAR(100) NOT NULL UNIQUE
);

-- 2. Tabla de Clientes
CREATE TABLE IF NOT EXISTS cliente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    rut INT NOT NULL,
    dvrut VARCHAR(1) NOT NULL
);

-- 3. Tabla de Productos (Relacionada con CategoriaProducto)
CREATE TABLE IF NOT EXISTS producto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    precio DOUBLE NOT NULL,
    categoria_producto_id BIGINT,
    FOREIGN KEY (categoria_producto_id) REFERENCES CategoriaProducto(id)
);

-- 4. Tabla de Bodega (Relacionada con Producto)
CREATE TABLE IF NOT EXISTS bodega (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock INT NOT NULL,
    producto_id BIGINT UNIQUE,
    FOREIGN KEY (producto_id) REFERENCES producto(id)
);

-- 5. Tabla de Ventas (Relacionada con Cliente)
CREATE TABLE IF NOT EXISTS venta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo_envio VARCHAR(50),
    monto DOUBLE NOT NULL,
    cliente_id BIGINT,
    FOREIGN KEY (cliente_id) REFERENCES cliente(id)
);

-- 6. Tabla de Detalles de Venta (Relacionada con Venta y Producto)
CREATE TABLE IF NOT EXISTS detalle_venta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cantidad INT NOT NULL,
    precio_unitario DOUBLE NOT NULL,
    producto_id BIGINT,
    venta_id BIGINT,
    FOREIGN KEY (producto_id) REFERENCES producto(id),
    FOREIGN KEY (venta_id) REFERENCES venta(id)
);

-- 7. Tabla de Boletas (Relacionada con Venta)
CREATE TABLE IF NOT EXISTS boleta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    monto DOUBLE NOT NULL,
    fecha DATE NOT NULL,
    venta_id BIGINT UNIQUE,
    FOREIGN KEY (venta_id) REFERENCES venta(id)
);

-- Reactivar chequeo de llaves foráneas
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- INSERTAR CATEGORÍAS DE PRODUCTOS
-- ============================================================

INSERT INTO CategoriaProducto (categoria) VALUES
('Perfumes'),
('Maquillaje'),
('Jabones'),
('Botellas')
ON DUPLICATE KEY UPDATE categoria = VALUES(categoria);

-- ============================================================
-- 2. INSERTAR PRODUCTOS POR CATEGORIA
-- ============================================================

-- CATEGORIA 1: PERFUMES
INSERT INTO producto (nombre, precio, categoria_producto_id) VALUES
('Perfume de Eucalipto natural', 8500, 1),
('Colonia Lavanda Premium', 6200, 1),
('Eau de Toilette Rosa Silvestre', 7800, 1),
('Perfume Flores Tropicales', 9500, 1);

-- CATEGORIA 2: MAQUILLAJE
INSERT INTO producto (nombre, precio, categoria_producto_id) VALUES
('Base Maquillante SPF 30', 12000, 2),
('Mascara de Pestanas Volumen', 8900, 2),
('Labial Rojo Intenso', 5600, 2),
('Rubor Natural Peachy', 6800, 2),
('Sombras de Ojos Set 12 Colores', 15400, 2);

-- CATEGORIA 3: JABONES
INSERT INTO producto (nombre, precio, categoria_producto_id) VALUES
('Jabon de Mano Natural', 5500, 3),
('Jabon Corporal Exfoliante', 7200, 3),
('Jabon Liquido Neutro 500ml', 4800, 3),
('Jabon Artesanal Organico', 6500, 3);

-- CATEGORIA 4: BOTELLAS
INSERT INTO producto (nombre, precio, categoria_producto_id) VALUES
('Botella Vidrio 500ml', 3200, 4),
('Botella Plastica 1L', 2500, 4),
('Botella Termica Acero 750ml', 18900, 4),
('Botella Reutilizable Bambu 600ml', 9800, 4),
('Botella Deportiva Flip Top 1L', 5600, 4);

-- ============================================================
-- 3. CREAR BODEGA (INVENTARIO) PARA TODOS LOS PRODUCTOS
-- ============================================================

INSERT INTO bodega (stock, producto_id) VALUES
(50, 1),
(40, 2),
(35, 3),
(30, 4),
(60, 5),
(75, 6),
(80, 7),
(70, 8),
(25, 9),
(150, 10),
(120, 11),
(180, 12),
(90, 13),
(200, 14),
(250, 15),
(40, 16),
(60, 17),
(100, 18)
ON DUPLICATE KEY UPDATE stock = VALUES(stock);

-- ============================================================
-- 4. CREAR CLIENTES DE EJEMPLO
-- ============================================================

INSERT INTO cliente (nombres, apellidos, rut, dvrut) VALUES
('Maria', 'Gonzalez Lopez', 12345678, '9'),
('Juan', 'Perez Rodriguez', 23456789, '5'),
('Ana', 'Martinez Garcia', 34567890, 'K'),
('Carlos', 'Sanchez Hernandez', 45678901, '4'),
('Sofia', 'Ruiz Fernandez', 56789012, '8'),
('Pedro', 'Lopez Ramirez', 67890123, '1'),
('Isabel', 'Garcia Torres', 78901234, '6'),
('Diego', 'Rodriguez Silva', 89012345, '3')
ON DUPLICATE KEY UPDATE apellidos = VALUES(apellidos);

-- ============================================================
-- 5. CREAR VENTAS DE EJEMPLO
-- ============================================================

INSERT INTO venta (tipo_envio, monto, cliente_id) VALUES
('Retiro en tienda', 23000, 1),
('Envio a domicilio', 14000, 2),
('Retiro en tienda', 31700, 3),
('Envio a domicilio', 45600, 4),
('Retiro en tienda', 28300, 5),
('Envio a domicilio', 19500, 6),
('Retiro en tienda', 62400, 7),
('Retiro en tienda', 51800, 8);

-- ============================================================
-- 6. CREAR DETALLES DE VENTAS
-- ============================================================

INSERT INTO detalle_venta (cantidad, precio_unitario, producto_id, venta_id) VALUES
(1, 5500, 10, 1),
(2, 8500, 1, 1);

INSERT INTO detalle_venta (cantidad, precio_unitario, producto_id, venta_id) VALUES
(1, 5500, 10, 2),
(1, 8500, 1, 2);

INSERT INTO detalle_venta (cantidad, precio_unitario, producto_id, venta_id) VALUES
(1, 12000, 5, 3),
(1, 8900, 6, 3),
(2, 5600, 7, 3);

INSERT INTO detalle_venta (cantidad, precio_unitario, producto_id, venta_id) VALUES
(2, 8500, 1, 4),
(1, 9500, 4, 4),
(2, 7200, 11, 4),
(1, 15400, 9, 4);

INSERT INTO detalle_venta (cantidad, precio_unitario, producto_id, venta_id) VALUES
(5, 3200, 14, 5),
(2, 9800, 17, 5);

INSERT INTO detalle_venta (cantidad, precio_unitario, producto_id, venta_id) VALUES
(1, 6200, 2, 6),
(1, 7800, 3, 6),
(1, 5600, 18, 6);

INSERT INTO detalle_venta (cantidad, precio_unitario, producto_id, venta_id) VALUES
(3, 5500, 10, 7),
(2, 7200, 11, 7),
(4, 2500, 15, 7),
(1, 18900, 16, 7);

INSERT INTO detalle_venta (cantidad, precio_unitario, producto_id, venta_id) VALUES
(1, 12000, 5, 8),
(1, 6800, 8, 8),
(2, 8500, 1, 8),
(1, 6200, 2, 8);

-- ============================================================
-- 7. CREAR BOLETAS PARA TODAS LAS VENTAS
-- ============================================================

INSERT INTO boleta (monto, fecha, venta_id) VALUES
(23000, '2026-05-06', 1),
(14000, '2026-05-06', 2),
(31700, '2026-05-06', 3),
(45600, '2026-05-06', 4),
(28300, '2026-05-06', 5),
(19500, '2026-05-06', 6),
(62400, '2026-05-06', 7),
(51800, '2026-05-06', 8);