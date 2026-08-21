# 🛠️ Ferretería Web (`toolboxcr`)

Sistema de comercio electrónico y gestión integral para ferreterías, desarrollado con **Spring Boot**, **Thymeleaf**, **Spring Security**, **JPA/Hibernate** y **MySQL**.

---

## Tabla de Contenidos
1. [Requisitos Previos](#-requisitos-previos)
2. [Instalación y Configuración](#-instalación-y-configuración)
3. [Ejecución de la Aplicación](#-ejecución-de-la-aplicación)
4. [Usuarios de Prueba y Roles](#-usuarios-de-prueba-y-roles)
5. [Descripción de Módulos](#-descripción-de-módulos)
6. [Internacionalización (i18n)](#-internacionalización-i18n)
7. [Tecnologías Utilizadas](#-tecnologías-utilizadas)

---

## Requisitos Previos

Asegúrate de contar con los siguientes componentes instalados en tu sistema:
- **Java Development Kit (JDK)**: Versión 17 o superior.
- **Apache Maven**: Versión 3.8.0 o superior.
- **MySQL Server**: Versión 8.0 o superior (o acceso a una instancia de MySQL en la nube como Aiven).
- **Navegador Web**: Google Chrome, Mozilla Firefox o Microsoft Edge.

---

## Instalación y Configuración

### 1. Clonar el Repositorio
```bash
git clone <URL_DEL_REPOSITORIO>
cd ferreteriadesarolloweb/InitializrSpringbootProject
```

### 2. Configurar la Base de Datos
1. Crea la base de datos e importa el script SQL incluido en `src/main/resources/ferreteria_web(1).sql`:
   ```sql
   CREATE DATABASE ferreteria_web CHARACTER SET utf8mb4 COLLATE utf8mb4_spanish_ci;
   USE ferreteria_web;
   SOURCE src/main/resources/ferreteria_web(1).sql;
   ```
2. Edita el archivo `src/main/resources/application.properties` con tus credenciales de base de datos:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/ferreteria_web
   spring.datasource.username=tu_usuario_mysql
   spring.datasource.password=tu_contraseña_mysql
   ```

### 3. Variables de Entorno
Para la integración con **Firebase Storage** (imágenes) y **Stripe** (pagos), puedes configurar las siguientes variables de entorno:
- `FIREBASE_BUCKET_NAME`
- `FIREBASE_STORAGE_PATH`
- `FIREBASE_JSON_CONTENT`
- `STRIPE_SECRET_KEY`

---

## Ejecución de la Aplicación

### Compilar el Proyecto
```bash
mvn clean compile
```

### Iniciar el Servidor de Desarrollo
```bash
mvn spring-boot:run
```

La aplicación estará disponible por defecto en:
**`http://localhost:9202`**

---

## Usuarios de Prueba y Roles

El sistema cuenta con un control de acceso basado en roles (**RBAC**) gestionado por Spring Security.

| Rol | Correo de Ejemplo | Contraseña | Permisos y Accesos |
| :--- | :--- | :--- | :--- |
| **Administrador / Dueño** | `admin@ferreteria.cr` | `Admin123!` | Acceso total: gestión de catálogo, carga masiva CSV, gestión de roles, reportes de ventas, cupones e inventario. |
| **Cliente** | `cliente@ferreteria.cr` | `Cliente123!` | Navegación de catálogo, carrito de compras, direcciones de envío, pedidos y lista de favoritos. |

> **Nota**: Puedes registrar nuevos usuarios directamente desde la pantalla de **Registro** (`/registro`). Por defecto, los usuarios recién registrados adquieren el rol `cliente`. Un usuario con rol `administrador` o `dueño` puede cambiar el rol de cualquier usuario en el módulo **Gestión de Roles** (`/usuario_rol/mantenimiento`).

---

##  Descripción de Módulos

### 1. Catálogo de Productos y Búsqueda
- **Exploración**: Filtros dinámicos por categoría, rango de precios y disponibilidad.
- **Búsqueda**: Búsqueda por nombre o código SKU.
- **Ficha Técnica**: Vista detallada con múltiples imágenes, especificaciones técnicas, precio de oferta (con cálculo de descuento %) y productos relacionados.
- **Comparador**: Herramienta visual para comparar especificaciones de hasta 4 productos simultáneamente.

### 2. Carrito de Compras y Checkout
- **Carrito**: Adición de productos con validación de stock en tiempo real.
- **Cupones de Descuento**: Aplicación de códigos promocionales (descuento porcentual o monto fijo).
- **Impuestos y Envíos**: Cálculo automático del 13% de IVA costarricense y selección de métodos de envío (Estándar, Express o Retiro en Tienda).
- **Procesamiento de Pago**: Integración con pasarela de pago (Stripe / SINPE / Gateway).

### 3. Cuenta de Usuario y Seguridad (RBAC)
- **Autenticación**: Registro e inicio de sesión seguro con encriptación BCrypt.
- **Protección contra Fuerza Bruta**: Bloqueo automático de cuenta tras 5 intentos fallidos consecutivas.
- **Validaciones**: Confirmación de contraseña y restricción del número telefónico a máximo 8 dígitos (norma Costa Rica).
- **Direcciones y Favoritos**: Administración de direcciones de entrega (con opción predeterminada) y lista de deseos (favoritos).

### 4. Inventario y Control de Stock
- **Sincronización Total**: Todos los productos del catálogo cuentan con un registro de inventario asociado automáticamente.
- **Ajustes Manuales**: Permite modificar el stock disponible indicando el motivo/justificación del ajuste.
- **Alertas de Stock Bajo**: Notificaciones automáticas cuando el stock sea menor o igual al umbral mínimo configurado.

### 5. Administración, Cupones y Reportes
- **Carga Masiva (CSV)**: Importación de catálogo desde archivos CSV.
- **Gestión de Pedidos**: Control de estados de pedidos (Pendiente, Preparando, Enviado, Entregado, Cancelado).
- **Reportes de Ventas**: Visualización de ventas por día, semana y mes, ticket promedio y ranking de productos más vendidos.
- **Gestión de Roles**: Asignación y revocación de permisos por correo de usuario.

---

## Internacionalización (i18n)

El sistema soporta **5 idiomas** accesibles desde el menú superior:
- 🇪🇸 **Español** (`es`) — *Predeterminado*
- 🇺🇸 **Inglés** (`en`)
- 🇮🇹 **Italiano** (`it`)
- 🇵🇹 **Portugués** (`pt`)
- 🇫🇷 **Francés** (`fr`)

---

## Tecnologías Utilizadas

- **Backend**: Java 17, Spring Boot 3.x, Spring Data JPA, Spring Security.
- **Frontend**: HTML5, Thymeleaf, JavaScript (Vanilla), CSS3, Bootstrap 5, FontAwesome 6.
- **Base de Datos**: MySQL 8.0, Triggers, Vistas y Restricciones de Integridad.
- **Servicios Externos**: Firebase Storage (Imágenes) y Stripe API (Pagos).
