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
CREATE TABLE IF NOT EXISTS categoria_producto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    categoria VARCHAR(100) NOT NULL UNIQUE
);

-- 2. Tabla de Clientes
CREATE TABLE IF NOT EXISTS cliente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    rut INT NOT NULL,
    dvrut VARCHAR(1) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    contrasena VARCHAR(150) NOT NULL,
    rol VARCHAR(20) NOT NULL DEFAULT 'CLIENTE'
);

-- 3. Tabla de Direcciones (Relacionada con Cliente)
CREATE TABLE IF NOT EXISTS direccion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    calle VARCHAR(150) NOT NULL,
    numero VARCHAR(20) NOT NULL,
    comuna VARCHAR(100) NOT NULL,
    ciudad VARCHAR(100) NOT NULL,
    region VARCHAR(100) NOT NULL,
    referencia VARCHAR(255),
    principal BOOLEAN DEFAULT FALSE,
    cliente_id BIGINT,
    FOREIGN KEY (cliente_id) REFERENCES cliente(id)
);

-- 4. Tabla de Contactos (Formulario publico)
CREATE TABLE IF NOT EXISTS contacto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    mensaje TEXT NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Tabla de Sesiones de Cliente
CREATE TABLE IF NOT EXISTS sesion_cliente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(100) NOT NULL UNIQUE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cliente_id BIGINT NOT NULL,
    FOREIGN KEY (cliente_id) REFERENCES cliente(id)
);

-- 3. Tabla de Productos (Relacionada con categoria_producto)
CREATE TABLE IF NOT EXISTS producto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    precio DOUBLE NOT NULL,
    categoria_producto_id BIGINT,
    FOREIGN KEY (categoria_producto_id) REFERENCES categoria_producto(id)
);

-- 4. Tabla de Carritos (Relacionada con Cliente y Producto)
CREATE TABLE IF NOT EXISTS carrito (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL UNIQUE,
    FOREIGN KEY (cliente_id) REFERENCES cliente(id)
);

CREATE TABLE IF NOT EXISTS carrito_producto (
    carrito_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    PRIMARY KEY (carrito_id, producto_id),
    FOREIGN KEY (carrito_id) REFERENCES carrito(id),
    FOREIGN KEY (producto_id) REFERENCES producto(id)
);

-- 4. Tabla de Stock por Producto
CREATE TABLE IF NOT EXISTS stock_producto (
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
    direccion_id BIGINT,
    FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    FOREIGN KEY (direccion_id) REFERENCES direccion(id)
);

-- 6. Tabla de Detalles de Venta (Relacionada con Venta y Producto)
CREATE TABLE IF NOT EXISTS detalle_venta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cantidad INT NOT NULL,
    precio_unitario DOUBLE NOT NULL,
    fecha DATE NOT NULL,
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

INSERT INTO categoria_producto (categoria) VALUES
('Perfumes'),
('Bloqueadores'),
('Jabones'),
('Botellas'),
('Aceites Herbales')
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

-- CATEGORIA 2: BLOQUEADORES
INSERT INTO producto (nombre, precio, categoria_producto_id) VALUES
('Bloqueador Solar Mineral SPF 30', 11900, 2),
('Bloqueador Solar Facial SPF 50', 13900, 2),
('Protector Solar Corporal SPF 50', 15400, 2),
('Bloqueador Infantil Hipoalergenico SPF 50', 16200, 2),
('Gel Solar Aloe Vera SPF 30', 9800, 2);

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

-- CATEGORIA 5: ACEITES HERBALES
INSERT INTO producto (nombre, precio, categoria_producto_id) VALUES
('Aceite Herbal de Lavanda', 7900, 5),
('Aceite Herbal de Romero', 7400, 5),
('Aceite Herbal de Manzanilla', 8200, 5),
('Aceite Herbal Relajante', 9200, 5);

-- ============================================================
-- 3. CREAR STOCK PARA TODOS LOS PRODUCTOS
-- ============================================================

INSERT INTO stock_producto (stock, producto_id) VALUES
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
(100, 18),
(55, 19),
(65, 20),
(45, 21),
(35, 22)
ON DUPLICATE KEY UPDATE stock = VALUES(stock);

-- ============================================================
-- 4. CREAR CLIENTES DE EJEMPLO
-- ============================================================

INSERT INTO cliente (nombres, apellidos, rut, dvrut, email, contrasena, rol) VALUES
('Maria', 'Gonzalez Lopez', 12345678, '9', 'maria@ecomarket.cl', '123456', 'CLIENTE'),
('Juan', 'Perez Rodriguez', 23456789, '5', 'juan@ecomarket.cl', '123456', 'CLIENTE'),
('Ana', 'Martinez Garcia', 34567890, 'K', 'ana@ecomarket.cl', '123456', 'CLIENTE'),
('Carlos', 'Sanchez Hernandez', 45678901, '4', 'carlos@ecomarket.cl', '123456', 'CLIENTE'),
('Sofia', 'Ruiz Fernandez', 56789012, '8', 'sofia@ecomarket.cl', '123456', 'CLIENTE'),
('Pedro', 'Lopez Ramirez', 67890123, '1', 'pedro@ecomarket.cl', '123456', 'CLIENTE'),
('Isabel', 'Garcia Torres', 78901234, '6', 'isabel@ecomarket.cl', '123456', 'CLIENTE'),
('Diego', 'Rodriguez Silva', 89012345, '3', 'diego@ecomarket.cl', '123456', 'CLIENTE')
ON DUPLICATE KEY UPDATE
    apellidos = VALUES(apellidos),
    email = VALUES(email),
    contrasena = VALUES(contrasena),
    rol = VALUES(rol);

-- ============================================================
-- 5. CREAR DIRECCIONES DE EJEMPLO
-- ============================================================

INSERT INTO direccion (id, calle, numero, comuna, ciudad, region, referencia, principal, cliente_id) VALUES
(1, 'Av. Providencia', '1234', 'Providencia', 'Santiago', 'Region Metropolitana', 'Depto 502', TRUE, 1),
(2, 'Los Aromos', '455', 'Nunoa', 'Santiago', 'Region Metropolitana', 'Casa azul', FALSE, 1),
(3, 'Av. Las Condes', '8900', 'Las Condes', 'Santiago', 'Region Metropolitana', 'Conserjeria torre B', TRUE, 2),
(4, 'Calle Larga', '77', 'Valparaiso', 'Valparaiso', 'Valparaiso', 'Frente a plaza central', TRUE, 3),
(5, 'Los Pinos', '310', 'Concepcion', 'Concepcion', 'Biobio', 'Porton negro', TRUE, 4),
(6, 'Camino del Sol', '22', 'La Serena', 'La Serena', 'Coquimbo', 'Parcela 8', TRUE, 5),
(7, 'Miraflores', '640', 'Temuco', 'Temuco', 'La Araucania', 'Segundo piso', TRUE, 6),
(8, 'O Higgins', '1501', 'Puerto Montt', 'Puerto Montt', 'Los Lagos', 'Local interior', TRUE, 7),
(9, 'Av. Alemania', '980', 'Valdivia', 'Valdivia', 'Los Rios', 'Edificio Los Rios', TRUE, 8)
ON DUPLICATE KEY UPDATE
    calle = VALUES(calle),
    numero = VALUES(numero),
    comuna = VALUES(comuna),
    ciudad = VALUES(ciudad),
    region = VALUES(region),
    referencia = VALUES(referencia),
    principal = VALUES(principal),
    cliente_id = VALUES(cliente_id);

-- ============================================================
-- 6. CREAR VENTAS DE EJEMPLO
-- ============================================================

INSERT INTO venta (tipo_envio, monto, cliente_id, direccion_id) VALUES
('Retiro en tienda', 23000, 1, 1),
('Envio a domicilio', 14000, 2, 3),
('Retiro en tienda', 31700, 3, 4),
('Envio a domicilio', 45600, 4, 5),
('Retiro en tienda', 28300, 5, 6),
('Envio a domicilio', 19500, 6, 7),
('Retiro en tienda', 62400, 7, 8),
('Retiro en tienda', 51800, 8, 9);

-- ============================================================
-- 7. CREAR DETALLES DE VENTAS
-- ============================================================

INSERT INTO detalle_venta (cantidad, precio_unitario, fecha, producto_id, venta_id) VALUES
(1, 5500, '2026-01-14', 10, 1),
(2, 8500, '2026-01-14', 1, 1);

INSERT INTO detalle_venta (cantidad, precio_unitario, fecha, producto_id, venta_id) VALUES
(1, 5500, '2026-02-03', 10, 2),
(1, 8500, '2026-02-03', 1, 2);

INSERT INTO detalle_venta (cantidad, precio_unitario, fecha, producto_id, venta_id) VALUES
(1, 12000, '2026-02-27', 5, 3),
(1, 8900, '2026-02-27', 6, 3),
(2, 5600, '2026-02-27', 7, 3);

INSERT INTO detalle_venta (cantidad, precio_unitario, fecha, producto_id, venta_id) VALUES
(2, 8500, '2026-03-18', 1, 4),
(1, 9500, '2026-03-18', 4, 4),
(2, 7200, '2026-03-18', 11, 4),
(1, 15400, '2026-03-18', 9, 4);

INSERT INTO detalle_venta (cantidad, precio_unitario, fecha, producto_id, venta_id) VALUES
(5, 3200, '2026-04-09', 14, 5),
(2, 9800, '2026-04-09', 17, 5);

INSERT INTO detalle_venta (cantidad, precio_unitario, fecha, producto_id, venta_id) VALUES
(1, 6200, '2026-04-25', 2, 6),
(1, 7800, '2026-04-25', 3, 6),
(1, 5600, '2026-04-25', 18, 6);

INSERT INTO detalle_venta (cantidad, precio_unitario, fecha, producto_id, venta_id) VALUES
(3, 5500, '2026-05-06', 10, 7),
(2, 7200, '2026-05-06', 11, 7),
(4, 2500, '2026-05-06', 15, 7),
(1, 18900, '2026-05-06', 16, 7);

INSERT INTO detalle_venta (cantidad, precio_unitario, fecha, producto_id, venta_id) VALUES
(1, 12000, '2026-05-29', 5, 8),
(1, 6800, '2026-05-29', 8, 8),
(2, 8500, '2026-05-29', 1, 8),
(1, 6200, '2026-05-29', 2, 8);

-- ============================================================
-- 8. CREAR BOLETAS PARA TODAS LAS VENTAS
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
