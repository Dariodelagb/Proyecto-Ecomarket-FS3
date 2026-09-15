# Informe Tecnico del Proyecto EcoMarket FS3

## 1. Introduccion

EcoMarket FS3 es una aplicacion web orientada a la gestion y venta de productos sustentables. El sistema integra una interfaz web para clientes, un dashboard administrativo, un backend desarrollado con Spring Boot, una base de datos MySQL y un microservicio independiente para reportes.

El proyecto esta containerizado con Docker Compose, lo que permite levantar todos los servicios de forma coordinada y reproducible. La aplicacion utiliza comunicacion HTTP mediante API REST entre frontend, backend y sistema de reportes.

## 2. Arquitectura general del sistema

La arquitectura se divide en los siguientes servicios principales:

- Frontend `frontend_node`: aplicacion web servida con Nginx en el puerto `8083`.
- Backend `db`: API REST principal desarrollada en Spring Boot, expuesta en el puerto `8080`.
- Base de datos `mysql`: servidor MySQL encargado de persistir la informacion del sistema.
- Sistema de reportes `sistemareportes`: microservicio Spring Boot expuesto en el puerto `8082`.

El flujo principal es:

```text
Usuario -> Frontend -> API REST Backend -> MySQL
                         ^
                         |
                 Sistema de reportes
```

El frontend no accede directamente a MySQL. Todas las operaciones pasan por el backend mediante endpoints REST. El sistema de reportes tampoco consulta directamente la base de datos, sino que consume datos desde el backend principal, manteniendo una separacion clara entre servicios.

## 3. Diseno de microservicios

El sistema utiliza una separacion por responsabilidades:

- El backend principal administra entidades de negocio como clientes, productos, categorias, direcciones, carritos, ventas, stock y contactos.
- El microservicio de reportes se encarga exclusivamente de consultar informacion consolidada y generar archivos de reporte.
- La base de datos centraliza la persistencia.
- El frontend se enfoca en presentacion, experiencia de usuario y consumo de la API.

Esta organizacion permite que cada componente tenga una funcion definida. Por ejemplo, si se requiere mejorar el sistema de reportes, se puede trabajar sobre `sistemareportes` sin modificar directamente la logica de ventas o productos del backend principal.

Docker Compose coordina el arranque de los servicios mediante `depends_on` y `healthcheck`, buscando que MySQL este listo antes de iniciar el backend, y que los servicios consumidores esperen a que la API principal este disponible.

## 4. Backend y persistencia de datos

El backend principal esta desarrollado con Spring Boot y utiliza JPA/Hibernate para la persistencia de datos.

Las entidades Java representan tablas de la base de datos. Algunos ejemplos son:

- `Cliente`
- `Producto`
- `CategoriaProducto`
- `Direccion`
- `Carrito`
- `CarritoProducto`
- `Venta`
- `DetalleVenta`
- `StockProducto`
- `Contacto`

JPA permite mapear estas clases a tablas MySQL mediante anotaciones como:

- `@Entity`
- `@Table`
- `@Id`
- `@GeneratedValue`
- `@ManyToOne`
- `@OneToMany`
- `@OneToOne`
- `@EmbeddedId`

Los repositorios extienden `JpaRepository`, lo que permite realizar operaciones comunes sin escribir SQL manualmente:

```java
findAll()
findById()
save()
delete()
count()
```

El backend expone controladores REST mediante anotaciones como:

```java
@RestController
@RequestMapping
@GetMapping
@PostMapping
@DeleteMapping
@PatchMapping
```

Ejemplos de endpoints:

- `GET /api/productos`
- `POST /api/productos`
- `GET /api/categorias`
- `POST /api/auth/login`
- `POST /api/auth/registro`
- `GET /api/carritos/cliente/{clienteId}`
- `POST /api/carritos/cliente/{clienteId}/productos/{productoId}`
- `POST /api/ventas`
- `GET /api/ventas-completas`

La persistencia inicial se apoya en el script `01-init.sql`, que crea tablas y carga datos iniciales cuando el volumen de MySQL se crea por primera vez.

Ademas, el componente `DataInitializer.java` funciona como una migracion ligera al iniciar la aplicacion. Este componente crea o ajusta tablas y columnas necesarias cuando la base de datos ya existe, por ejemplo:

- Creacion de tabla `contacto`.
- Creacion de tabla `direccion`.
- Creacion de tabla `carrito_producto`.
- Agregado de columna `cantidad` al carrito.
- Agregado de columna `fecha` a `detalle_venta`.
- Carga de categorias base.

## 5. Estrategias utilizadas en backend

El backend utiliza varias estrategias para mantener ordenada la logica:

- Separacion entre modelos, repositorios y controlador.
- Uso de JPA para reducir SQL repetitivo.
- Uso de transacciones con `@Transactional` en operaciones que modifican varias entidades.
- Resolucion de relaciones reales antes de guardar ventas o productos.
- Validaciones simples antes de persistir datos.
- Uso de DTOs en el microservicio de reportes para no depender directamente de las entidades del backend principal.

Un ejemplo importante es el carrito. Inicialmente se manejaba como una relacion directa entre carrito y productos, pero se refactorizo para usar una entidad intermedia `CarritoProducto`, permitiendo guardar la cantidad de cada producto.

Esto permite representar correctamente:

```text
Carrito -> Producto + cantidad
```

y luego generar ventas con detalles que respetan esas cantidades.

## 6. Frontend y experiencia de usuario

El frontend esta construido como una aplicacion web estatica empaquetada con Webpack y servida por Nginx. Utiliza HTML, CSS y JavaScript modular.

Las vistas principales son:

- Home
- Productos
- Carrito
- Compra
- Registro
- Login
- Dashboard administrativo

El frontend consume la API REST mediante `fetch`. Por ejemplo:

- Carga productos desde `/api/productos`.
- Agrega productos al carrito desde `/api/carritos/cliente/{id}/productos/{id}`.
- Registra ventas en `/api/ventas`.
- Obtiene categorias en `/api/categorias`.
- Carga usuarios y ventas en el dashboard.

La interfaz publica utiliza un estilo visual orientado a productos naturales y sustentables. Se incorporaron:

- Hero visual.
- Fondo con imagen de bosque.
- Efecto de vidrio mate en tarjetas.
- Carousel de productos destacados.
- Agrupacion de productos por categoria.
- Controles de cantidad para agregar productos al carrito.
- Carrito con subtotal y cantidad por producto.
- Checkout con autocompletado de datos del cliente.

Tambien se integro Lenis para scroll suavizado, pero se agrego un boton para alternar entre Lenis y scroll nativo CSS, debido a que los efectos visuales como blur, fondos grandes y parallax pueden afectar el rendimiento.

## 7. Estrategias utilizadas en frontend

El frontend sigue una estrategia modular. Los comportamientos estan separados en archivos dentro de `src/js/components`.

Ejemplos:

- `home-loader.js`: carga productos destacados en la home.
- `products-page-loader.js`: carga productos y gestiona cantidad para carrito.
- `cart-page-loader.js`: renderiza el carrito.
- `checkout-loader.js`: procesa la compra.
- `auth-loader.js`: gestiona sesion, login, logout y roles.
- `metrics-loader.js`: carga metricas del dashboard.
- `products-loader.js`: carga tabla de productos en dashboard.
- `users-loader.js`: carga tabla de usuarios.
- `smooth-scroll-loader.js`: controla Lenis y scroll nativo.
- `home-bg-parallax-loader.js`: aplica parallax al fondo.

Esta separacion permite modificar una funcionalidad sin afectar toda la aplicacion.

El frontend tambien utiliza `localStorage` para guardar informacion de sesion y preferencias visuales, como el modo de scroll.

## 8. Aplicacion de patrones de diseno

El proyecto aplica varios patrones y principios de diseno de software:

### MVC en backend

Spring Boot organiza el backend siguiendo una separacion similar a MVC:

- Modelo: entidades JPA.
- Controlador: endpoints REST.
- Repositorio: acceso a datos.

Aunque no hay una capa de servicios extensa en el backend principal, la separacion entre controlador, entidad y repositorio ya permite una estructura clara.

### Repository Pattern

Los repositorios JPA funcionan como una implementacion del patron Repository. Encapsulan el acceso a datos y evitan que el controlador tenga que escribir SQL directamente.

### DTO Pattern

El microservicio de reportes utiliza DTOs para recibir informacion del backend principal. Esto evita acoplar el microservicio directamente a las entidades internas del servicio `db`.

### Facade / Service Layer

En `sistemareportes`, clases como `ReporteService`, `ReportePruebasService`, `InfraestructuraPruebasService` y `CoberturaCodigoService` funcionan como capas de servicio que encapsulan tareas especificas:

- Generar Excel.
- Ejecutar verificaciones.
- Probar conectividad.
- Leer cobertura JaCoCo.

### Component-based frontend

Aunque no se usa un framework como React, el frontend se organiza por componentes JavaScript y parciales HTML reutilizables. Esto permite una estructura similar a componentes:

- Tablas.
- Metric cards.
- Formularios.
- Loaders de datos.
- Renderizadores de productos.

## 9. Integracion frontend, backend y base de datos

La integracion entre capas se realiza mediante API REST.

Cuando un usuario ve productos:

```text
productos.html -> GET /api/productos -> Backend -> MySQL
```

Cuando agrega productos al carrito:

```text
Frontend -> POST /api/carritos/cliente/{clienteId}/productos/{productoId}
```

El backend recibe el producto y la cantidad, luego actualiza la tabla `carrito_producto`.

Cuando se completa una compra:

```text
Checkout -> POST /api/ventas -> Backend -> MySQL
```

La venta guarda:

- Cliente.
- Direccion.
- Tipo de envio.
- Monto total.
- Detalles de productos.
- Cantidades.
- Precio unitario.

La base de datos mantiene la relacion entre ventas y productos mediante `detalle_venta`.

## 10. Sistema de reportes

El microservicio `sistemareportes` genera reportes a partir de los datos del backend principal.

Endpoints importantes:

- `GET /api/reportes/resumen`
- `GET /get-reporte`
- `GET /api/reportes/get-reporte`
- `GET /get-pruebas`
- `GET /api/reportes/get-pruebas`

El endpoint `/get-reporte` descarga un archivo Excel generado con Apache POI. El reporte incluye:

- Resumen del dashboard.
- Ventas.
- Usuarios sin datos sensibles.
- Productos y stock.
- Ventas mensuales.

Para obtener los datos, el microservicio usa clientes Feign:

- `ClientesClient`
- `InventarioClient`
- `VentasClient`

Estos clientes consumen endpoints del backend principal en lugar de acceder directamente a MySQL.

Esto mantiene la responsabilidad de la base de datos dentro del servicio principal y permite que reportes sea un consumidor de la API.

## 11. Pruebas unitarias y cobertura

El servicio de reportes incorpora pruebas unitarias en `src/test`.

Pruebas implementadas:

- Calculo de resumen de ventas y productos.
- Generacion del archivo Excel.
- Validacion de hojas principales del Excel.
- Verificacion de que usuarios no exponga datos sensibles.
- Verificacion de que ventas incluya IDs.
- Validacion del reporte de pruebas.
- Lectura y calculo de cobertura desde JaCoCo.

Se integro JaCoCo para generar cobertura de codigo al ejecutar:

```bash
./mvnw test
```

o en Windows:

```powershell
.\mvnw.cmd test
```

El reporte queda disponible en:

```text
target/site/jacoco/index.html
```

Tambien existe el endpoint:

```text
GET /get-pruebas
```

Este devuelve un archivo de texto con:

- Resultado de conexion a MySQL.
- Validacion de API REST.
- Validacion de endpoints principales.
- Generacion de Excel.
- Resumen de cobertura JaCoCo.

## 12. Uso de GitHub y control de versiones

GitHub se utiliza como plataforma de almacenamiento remoto del proyecto y control de versiones.

El uso de Git permite:

- Registrar cambios progresivos del codigo.
- Volver a versiones anteriores si ocurre un error.
- Revisar diferencias entre cambios.
- Mantener respaldo remoto.
- Facilitar despliegue en servidores.
- Compartir el proyecto para revision o evaluacion.

Una estrategia recomendada para este proyecto es trabajar con commits separados por funcionalidad:

```text
feat: agregar carrito con cantidades
feat: implementar sistema de reportes Excel
feat: agregar dashboard administrativo
fix: corregir carga de categorias
docs: actualizar README con endpoints Postman
test: agregar pruebas unitarias de reportes
```

Tambien se recomienda evitar subir archivos generados como:

- `node_modules`
- `target`
- `build`
- Archivos temporales

El repositorio incluye una coleccion Postman, lo que facilita probar los endpoints del backend y reportes desde otro equipo.

## 13. Docker y despliegue

El proyecto usa Docker Compose para levantar los servicios necesarios:

- MySQL.
- Backend principal.
- Frontend.
- Sistema de reportes.

Esto reduce diferencias entre entornos locales y servidores. En lugar de instalar manualmente Java, Node, MySQL y Nginx en cada equipo, Docker crea contenedores con las dependencias necesarias.

Comando principal:

```bash
docker compose up --build
```

Tambien se definieron healthchecks para mejorar el orden de arranque, especialmente entre MySQL y el backend.

## 14. Conclusiones

EcoMarket FS3 integra frontend, backend, base de datos y reportes mediante una arquitectura clara basada en servicios. El backend centraliza la logica de negocio y persistencia con Spring Boot, JPA y MySQL. El frontend consume la API REST y presenta una experiencia visual orientada a productos sustentables. El sistema de reportes funciona como microservicio independiente, generando archivos Excel y resultados de pruebas.

El proyecto aplica buenas practicas como separacion de responsabilidades, uso de repositorios, DTOs, servicios especializados, pruebas unitarias, cobertura de codigo, Docker Compose y control de versiones con GitHub.

Como mejoras futuras, se podria considerar:

- Incorporar Flyway o Liquibase para migraciones formales.
- Agregar autenticacion mas robusta con JWT.
- Encriptar contrasenas.
- Agregar roles y permisos a nivel de backend.
- Crear una API documentada con OpenAPI/Swagger.
- Automatizar pruebas en GitHub Actions.
- Separar mas claramente la capa de servicios en el backend principal.
