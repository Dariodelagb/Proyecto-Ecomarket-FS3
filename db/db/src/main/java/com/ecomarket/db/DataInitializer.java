package com.ecomarket.db;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/// <summary>
/// Componente de inicialización de datos para la base de datos de Ecomarket.
/// Este componente se ejecuta al iniciar la aplicación y se encarga de:
/// 1. Migrar el esquema de la tabla "cliente" para agregar campos de autentic
///   ación (email, contrasena, rol).
/// 2. Crear tablas necesarias para el funcionamiento de la base de datos.
/// 3. Sembrar datos iniciales en tablas como "categoria_producto", "direccion" y actualizar registros existentes.
/// </summary>

@Component
public class DataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        migrateClienteAuthSchema();
        migrateSesionSchema();
        migrateContactoSchema();
        migrateDireccionSchema();
        migrateCarritoSchema();
        migrateStockProductoSchema();
        migrateDetalleVentaFecha();
        seedCategorias();
        seedDirecciones();
        seedVentaDirecciones();
        seedDetalleVentaFechas();
    }

    private void migrateClienteAuthSchema() {
        Integer emailCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'cliente'
              AND COLUMN_NAME = 'email'
            """,
            Integer.class
        );

        if (emailCount != null && emailCount == 0) {
            jdbcTemplate.execute("ALTER TABLE cliente ADD COLUMN email VARCHAR(150)");
        }

        Integer passwordCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'cliente'
              AND COLUMN_NAME = 'contrasena'
            """,
            Integer.class
        );

        if (passwordCount != null && passwordCount == 0) {
            jdbcTemplate.execute("ALTER TABLE cliente ADD COLUMN contrasena VARCHAR(150)");
        }

        Integer roleCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'cliente'
              AND COLUMN_NAME = 'rol'
            """,
            Integer.class
        );

        if (roleCount != null && roleCount == 0) {
            jdbcTemplate.execute("ALTER TABLE cliente ADD COLUMN rol VARCHAR(20) DEFAULT 'CLIENTE'");
        }

        jdbcTemplate.update(
            """
            UPDATE cliente
            SET email = CONCAT('cliente', id, '@ecomarket.cl')
            WHERE email IS NULL OR email = ''
            """
        );

        jdbcTemplate.update(
            """
            UPDATE cliente
            SET contrasena = '123456'
            WHERE contrasena IS NULL OR contrasena = ''
            """
        );

        jdbcTemplate.update(
            """
            UPDATE cliente
            SET rol = 'CLIENTE'
            WHERE rol IS NULL OR rol = ''
            """
        );
    }

    private void migrateSesionSchema() {
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS sesion_cliente (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                token VARCHAR(100) NOT NULL UNIQUE,
                fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                cliente_id BIGINT NOT NULL
            )
            """
        );
    }

    private void migrateContactoSchema() {
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS contacto (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                nombre VARCHAR(150) NOT NULL,
                correo VARCHAR(150) NOT NULL,
                mensaje TEXT NOT NULL,
                fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """
        );
    }

    private void migrateDireccionSchema() {
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS direccion (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                calle VARCHAR(150) NOT NULL,
                numero VARCHAR(20) NOT NULL,
                comuna VARCHAR(100) NOT NULL,
                ciudad VARCHAR(100) NOT NULL,
                region VARCHAR(100) NOT NULL,
                referencia VARCHAR(255),
                principal BOOLEAN DEFAULT FALSE,
                cliente_id BIGINT
            )
            """
        );

        Integer columnCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'venta'
              AND COLUMN_NAME = 'direccion_id'
            """,
            Integer.class
        );

        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute("ALTER TABLE venta ADD COLUMN direccion_id BIGINT");
        }
    }

    private void migrateCarritoSchema() {
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS carrito (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                cliente_id BIGINT NOT NULL UNIQUE
            )
            """
        );

        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS carrito_producto (
                carrito_id BIGINT NOT NULL,
                producto_id BIGINT NOT NULL,
                cantidad INT NOT NULL DEFAULT 1,
                PRIMARY KEY (carrito_id, producto_id)
            )
            """
        );

        Integer cantidadColumnCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'carrito_producto'
              AND COLUMN_NAME = 'cantidad'
            """,
            Integer.class
        );

        if (cantidadColumnCount != null && cantidadColumnCount == 0) {
            jdbcTemplate.execute("ALTER TABLE carrito_producto ADD COLUMN cantidad INT NOT NULL DEFAULT 1");
        }
    }

    private void migrateStockProductoSchema() {
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS stock_producto (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                stock INT NOT NULL,
                producto_id BIGINT UNIQUE
            )
            """
        );

        Integer oldTableCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'bodega'
            """,
            Integer.class
        );

        if (oldTableCount != null && oldTableCount > 0) {
            jdbcTemplate.update(
                """
                INSERT INTO stock_producto (stock, producto_id)
                SELECT b.stock, b.producto_id
                FROM bodega b
                WHERE b.producto_id IS NOT NULL
                ON DUPLICATE KEY UPDATE stock = VALUES(stock)
                """
            );
        }
    }

    private void migrateDetalleVentaFecha() {
        Integer columnCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'detalle_venta'
              AND COLUMN_NAME = 'fecha'
            """,
            Integer.class
        );

        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute("ALTER TABLE detalle_venta ADD COLUMN fecha DATE");
        }
    }

    private void seedCategorias() {
        Object[][] categorias = {
            {1L, "Perfumes"},
            {2L, "Bloqueadores"},
            {3L, "Jabones"},
            {4L, "Botellas"},
            {5L, "Aceites Herbales"}
        };

        for (Object[] categoria : categorias) {
            jdbcTemplate.update(
                """
                INSERT INTO categoria_producto (id, categoria)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE categoria = VALUES(categoria)
                """,
                categoria[0],
                categoria[1]
            );
        }
    }

    private void seedDirecciones() {
        Object[][] direcciones = {
            {1L, "Av. Providencia", "1234", "Providencia", "Santiago", "Region Metropolitana", "Depto 502", true, 1L},
            {2L, "Los Aromos", "455", "Nunoa", "Santiago", "Region Metropolitana", "Casa azul", false, 1L},
            {3L, "Av. Las Condes", "8900", "Las Condes", "Santiago", "Region Metropolitana", "Conserjeria torre B", true, 2L},
            {4L, "Calle Larga", "77", "Valparaiso", "Valparaiso", "Valparaiso", "Frente a plaza central", true, 3L},
            {5L, "Los Pinos", "310", "Concepcion", "Concepcion", "Biobio", "Porton negro", true, 4L},
            {6L, "Camino del Sol", "22", "La Serena", "La Serena", "Coquimbo", "Parcela 8", true, 5L},
            {7L, "Miraflores", "640", "Temuco", "Temuco", "La Araucania", "Segundo piso", true, 6L},
            {8L, "O Higgins", "1501", "Puerto Montt", "Puerto Montt", "Los Lagos", "Local interior", true, 7L},
            {9L, "Av. Alemania", "980", "Valdivia", "Valdivia", "Los Rios", "Edificio Los Rios", true, 8L}
        };

        for (Object[] direccion : direcciones) {
            jdbcTemplate.update(
                """
                INSERT INTO direccion (id, calle, numero, comuna, ciudad, region, referencia, principal, cliente_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    calle = VALUES(calle),
                    numero = VALUES(numero),
                    comuna = VALUES(comuna),
                    ciudad = VALUES(ciudad),
                    region = VALUES(region),
                    referencia = VALUES(referencia),
                    principal = VALUES(principal),
                    cliente_id = VALUES(cliente_id)
                """,
                direccion
            );
        }
    }

    private void seedVentaDirecciones() {
        jdbcTemplate.update(
            """
            UPDATE venta
            SET direccion_id = CASE id
                WHEN 1 THEN 1
                WHEN 2 THEN 3
                WHEN 3 THEN 4
                WHEN 4 THEN 5
                WHEN 5 THEN 6
                WHEN 6 THEN 7
                WHEN 7 THEN 8
                WHEN 8 THEN 9
                ELSE direccion_id
            END
            WHERE direccion_id IS NULL
            """
        );
    }

    private void seedDetalleVentaFechas() {
        jdbcTemplate.update(
            """
            UPDATE detalle_venta
            SET fecha = CASE venta_id
                WHEN 1 THEN '2026-01-14'
                WHEN 2 THEN '2026-02-03'
                WHEN 3 THEN '2026-02-27'
                WHEN 4 THEN '2026-03-18'
                WHEN 5 THEN '2026-04-09'
                WHEN 6 THEN '2026-04-25'
                WHEN 7 THEN '2026-05-06'
                WHEN 8 THEN '2026-05-29'
                ELSE CURRENT_DATE
            END
            WHERE fecha IS NULL
            """
        );
    }
}
