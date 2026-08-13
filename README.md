# SnowTrail 🍦✨
### Localizador de Neverías y Sincronización en Tiempo Real (Android & Wear OS)

---

## 👥 Integrantes del Proyecto
* **Paola Jaqueline López Mata**
* **Gerardo Manzano Villafaña**
* **Jennifer Ailin Medina Hernández**

**Grupo:** *[GDS6092]*

---

## 🎯 Objetivo del Proyecto
Desarrollar una aplicación móvil integrada con un dispositivo wearable (Wear OS) con una **estética visual premium inspirada en tonos pastel de helados**. La plataforma permite buscar, ordenar y seguir pedidos en neverías cercanas en tiempo real mediante sincronización GPS, persistencia local SQLite y alertas personalizadas de proximidad y promociones en el reloj.

---

## ⚙️ Tecnologías Utilizadas
* **Lenguaje:** Kotlin (v2.2.10) & Java 17
* **Gestor de Dependencias y Catálogo:** Gradle Version Catalog (`gradle/libs.versions.toml`) con scripts en Kotlin DSL (`build.gradle.kts`).
* **Diseño de Interfaz:** Jetpack Compose (Móvil & Android TV) & Compose for Wear OS (Smartwatch).
* **Persistencia Local:** `SQLiteOpenHelper` nativo (Esquemas versionados: v5 en móvil, v7 en Android TV).
* **Capa de Comunicación y Red:** 
  * Servidor multihilo de Sockets TCP en puerto `9090` (Comunicación Móvil ➜ TV).
  * Google Play Services Wearable (`DataClient`) y difusiones locales (Comunicación Móvil ➜ Wear OS).
* **Geolocalización y Mapas:** API REST de Positionstack & Leaflet.js inyectado en `WebView`.
* **Animaciones y Efectos:** `Modifier.basicMarquee()` de Compose (efecto marquesina en el reloj), transiciones suaves y badges dinámicos.

---

## 🚀 Funcionalidades Principales

### 📱 Dispositivo Móvil (Celular)
1. **Pestaña GPS de Simulación:** Permite desplazar coordenadas mediante un deslizador para simular el movimiento real del usuario en el mapa, actualizando dinámicamente las neverías cercanas en un radio de 3 km.
2. **Pestaña de Neverías:** Listado interactivo de establecimientos con opciones para marcar favoritos en tiempo real y visualizar promociones especiales.
3. **Pestaña de Pedidos:** Flujo de compra completo, carrito interactivo y actualización de estados del pedido (Nuevo, Aceptado, Pospuesto, Entregado).
4. **Diseño Visual Pastel:** Interfaz estilizada en tonalidades suaves de vainilla, menta, durazno y fresa con textos legibles en marrón cacao.

### ⌚ Reloj Inteligente (Wear OS)
1. **Bandeja de Notificaciones con Marquesina:** Alertas con texto corrido continuo (`basicMarquee()`) para que los mensajes largos no se corten en la pantalla circular del reloj.
2. **Detalle a Pantalla Completa Instantáneo:** Al tocar una alerta, la pantalla cambia al instante mostrando el desglose completo del estatus, proximidad o promoción sin depender de interfaces deslizables laterales.
3. **Cupones de Promoción Enriquecidos:** Si la notificación es una oferta, el reloj genera de manera interactiva un cupón visual con diseño de línea discontinua y un código de descuento único (ej: `FRESA25`, `MINT30`) listo para mostrar en caja.
4. **Listado de Neverías Cercanas y Pedidos:** Visualización rápida con ordenamiento dinámico según la cercanía simulada en el celular.
5. **Navegación por Botones Físicos:** Soporte para botones laterales físicos (Stem Keys) para subir/bajar enfoque y confirmar/descartar alertas.

---

## 📸 Capturas de Pantalla

### 📱 1. Módulo Móvil (Smartphone) - `:app`

#### 🔐 Autenticación y Cuentas
| Iniciar Sesión | Registro de Nuevo Cliente |
| :---: | :---: |
| ![Iniciar Sesión](Capturas/Pantalla_login_app.png) | ![Registro de Cliente](Capturas/Pantalla_registro_app.png) |

#### 👤 Vistas del Cliente
| Explorar Neverías | Favoritos Personalizados | Detalle & Carrito de Helados |
| :---: | :---: | :---: |
| ![Explorar Neverías](Capturas/Pantalla_principal_app.png) | ![Favoritos](Capturas/Pantalla_favoritos_app.png) | ![Detalle y Carrito](Capturas/Pantalla_productos_app.png) |
| **Seguimiento de Pedido** | **Mapa Interactivo (Positionstack)** | **Simulador GPS de Proximidad** |
| ![Seguimiento Pedido](Capturas/Pantalla_pedido_app.png) | ![Mapa Cliente](Capturas/Pantalla_Mapa_app.png) | ![GPS Celular](Capturas/celular_gps.png) |

#### 🛠️ Vistas del Administrador
| Control de Neverías | Control de Promociones | Gestión de Estados de Pedidos |
| :---: | :---: | :---: |
| ![Control Neverías](Capturas/Pantalla_neverías_admin_app.png) | ![Control Promociones](Capturas/Pantalla_promociones_admin_app.png) | ![Historial Pedidos](Capturas/Pantalla_historialPedidos_admin_app.png) |
| **Alta de Nueva Nevería** | **Alta de Nueva Promoción** | **Nuevo Pedido Rápido con Carrito** |
| ![Alta Nevería](Capturas/Pantalla_nuevaNeveria_admin_app.png) | ![Alta Promoción](Capturas/Pantalla_nuevaPromocion_admin_app.png) | ![Nuevo Pedido](Capturas/Pantalla_nuevoPedido_admin_app.png) |
| **Mapa del Administrador** | | |
| ![Mapa Admin](Capturas/Pantalla_Mapa_admin_app.png) | | |

---

### ⌚ 2. Módulo Smartwatch (Wear OS) - `:wear`

| Neverías Cercanas en Reloj | Bandeja de Alertas (1) | Bandeja de Alertas (2) |
| :---: | :---: | :---: |
| ![Neverías Reloj](Capturas/reloj_neverias.png) | ![Notificaciones Reloj 1](Capturas/reloj_notificaciones.png) | ![Notificaciones Reloj 2](Capturas/reloj_notificaciones1.png) |
| **Cupón de Descuento Interactivo** | | |
| ![Cupón Reloj](Capturas/reloj_detalle.png) | | |

---

### 📺 3. Módulo Android TV - `:tv`

| Carrusel de 50 Promociones & Reloj en Vivo | Cola Dividida de Pedidos (Nuevos & en Preparación) |
| :---: | :---: |
| ![Pantalla Principal TV](Capturas/Pantalla_principal_tv.png) | ![Cola Pedidos TV](Capturas/Pantalla_pedidos_tv.png) |

---

## 🛠️ Instrucciones para Ejecutar el Proyecto

1. **Clonar el Repositorio:**
   ```bash
   git clone https://github.com/PaolaLpez/SnowTrail.git
   ```
2. **Abrir en Android Studio:**
   - Abre la carpeta `SnowTrail` en Android Studio (versión Jellyfish o superior recomendada).
   - Deja que Gradle descargue las dependencias y sincronice el proyecto.
3. **Instalación en Dispositivos/Emuladores:**
   - Ejecuta el módulo `:app` en tu dispositivo físico Android o emulador de teléfono.
   - Ejecuta el módulo `:wear` en tu emulador de reloj inteligente Wear OS (asegúrate de que ambos estén emparejados en el mismo puente de comunicación).
4. **Simular Movimiento:**
   - Ve a la pestaña **GPS** en el celular.
   - Desplaza el control de la ubicación para ver cómo cambian y se sincronizan las neverías en tiempo real en la pantalla del reloj.

---

## 📚 Documentación Técnica Detallada por Módulos

Para consultar el código fuente documentado, diagramas de arquitectura y desgloses archivo por archivo de cada plataforma, visita la documentación técnica especializada:

* [📱 **Módulo Móvil (Smartphone) - `:app`**](app/README.md): Arquitectura Jetpack Compose, roles Cliente/Admin, persistencia SQLite v5, mapa Positionstack + Leaflet y transmisor de sockets TCP.
* [⌚ **Módulo Smartwatch (Wear OS) - `:wear`**](wear/README.md): Compose for Wear OS, notificaciones con marquesina continua (`basicMarquee`), cupones de descuento interactivos y soporte de botones físicos.
* [📺 **Módulo Android TV - `:tv`**](tv/README.md): Servidor multihilo de sockets TCP (puerto 9090), layout dividido, cola de pedidos nuevos/pendientes con alertas y carrusel de 50 promociones únicas.

## Carta Firmada
[Carta .pdf](https://github.com/user-attachments/files/31048369/Carta.pdf)
