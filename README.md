# Ecomarket - Sistema de Gestión de Ventas

Proyecto desarrollado para la gestión de productos, inventarios y ventas, containerizado con **Docker** para garantizar consistencia entre entornos.

## Requisitos

- [Docker](https://www.docker.com/products/docker-desktop/).
- [Git](https://git-scm.com/).

---

## Cómo ejecutar el proyecto

Para clonar y levantar el sistema:

### 1. Clonar el repositorio
Abre la terminal y ejecuta:

git clone https://github.com/Dariodelagb/Proyecto-Ecomarket-FS3.git
cd Proyecto-Ecomarket-FS3

### 2. Levantar los contenedores
Para iniciar el proyecto, ejecuta el siguiente comando:

docker-compose up --build
Nota: Esto tarda bastante.

### 3. Acceso al sistema
Una vez que veas en la consola que todos los contenedores están corriendo:

Base de Datos: Accesible en el puerto 3306.

Aplicación: Accesible en http://localhost:8081.

### Persistencia de datos

Si deseas reiniciar la base de datos desde cero (limpiar todos los datos):

docker-compose down -v

docker-compose up --build

El script de inicialización (01-init.sql) se ejecuta automáticamente la primera vez que se crea el contenedor de base de datos.

El contenedor de la aplicación de ventas espera automáticamente a que la base de datos esté lista antes de arrancar.
