# ⌚ Módulo Wear OS (Smartwatch) - SnowTrail (`:wear`)

---

## 📌 Resumen de Arquitectura y Propósito
El módulo `:wear` está diseñado específicamente para dispositivos **Wear OS** con pantallas circulares o cuadradas. Proporciona una interfaz compacta y reactiva basada en **Compose for Wear OS**, enfocada en alertas de proximidad en tiempo real, seguimiento instantáneo del estado de pedidos y generación visual de cupones de descuento interactivos.

```mermaid
graph LR
    A[Móvil :app] -->|Broadcast / GPS Proximity| B[Wear OS :wear]
    B --> C[Bandeja de Notificaciones con Marquesina]
    B --> D[Cupones de Descuento Interactivos]
    B --> E[Lista de Neverías Cercanas]
```

---

## 📦 Configuración de Compilación y Dependencias (`libs.versions.toml` & `build.gradle.kts`)

### 1. Librerías de Wear OS en `gradle/libs.versions.toml`
El catálogo define las versiones y artefactos especializados para Wearable Compose y Play Services:

#### 💻 Fragmento de `libs.versions.toml`:
```toml
[versions]
playServicesWearable = "20.0.1"
composeMaterial3 = "1.5.6"
composeFoundation = "1.5.6"
wearToolingPreview = "1.0.0"

[libraries]
play-services-wearable = { group = "com.google.android.gms", name = "play-services-wearable", version.ref = "playServicesWearable" }
compose-material3 = { group = "androidx.wear.compose", name = "compose-material3", version.ref = "composeMaterial3" }
compose-foundation = { group = "androidx.wear.compose", name = "compose-foundation", version.ref = "composeFoundation" }
wear-tooling-preview = { group = "androidx.wear", name = "wear-tooling-preview", version.ref = "wearToolingPreview" }
```

---

### 2. Script de Construcción del Módulo (`wear/build.gradle.kts`)
Configura el compilador para soportar las librerías optimizadas para smartwatches:

#### 💻 Fragmento de `wear/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "mx.utng.snowtrail"
    compileSdk = 36

    defaultConfig {
        applicationId = "mx.utng.snowtrail"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.play.services.wearable)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
}
```

---

## 📂 Desglose Técnico Archivo por Archivo (`main` y componentes)

---

### 1. `MainActivity.kt` (`main` - Punto de Entrada)
* **Ubicación:** `wear/src/main/java/mx/utng/snowtrail/presentation/MainActivity.kt`
* **Propósito:** Actividad principal del reloj inteligente que implementa el ciclo de vida de Wear OS, soporte para modo ambiente (`AmbientMode`), navegación por botones físicos (*Stem Keys*) y renderizado de componentes adaptados a pantallas reducidas.

#### 🔧 Componentes y Funcionalidades Clave:
* **Efecto Marquesina (`basicMarquee`):** Permite el desplazamiento continuo de textos largos en títulos y alertas para que no se corten en la pantalla circular del reloj.
* **Modal de Cupones Enriquecidos:** Cuando una alerta corresponde a una promoción, renderiza un cupón con bordes punteados y código promocional (ej. `FRESA25`, `MINT30`) optimizado para mostrar en el mostrador de la heladería.
* **Manejo de Botones Físicos:** Captura eventos de teclas de hardware (`KEYCODE_STEM_1`, `KEYCODE_STEM_2`) para navegación y confirmación rápida de alertas sin tocar la pantalla.
* **Lista de Heladerías Cercanas:** Componente `ScalingLazyColumn` con escalado visual de elementos según la posición en la curvatura de la pantalla.

#### 💻 Fragmento Técnico de Código:
```kotlin
// Renderizado de Alerta con Texto en Marquesina Continua y Cupón
@Composable
fun WearNotificationItem(
    notification: MockNotification,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = notification.titulo,
                style = MaterialTheme.typography.title3,
                color = Color(0xFFEF9A9A),
                maxLines = 1,
                modifier = Modifier.basicMarquee() // Efecto de marquesina continua
            )
            Text(
                text = notification.mensaje,
                style = MaterialTheme.typography.body2,
                color = Color.White
            )
        }
    }
}
```

> [!NOTE]
> Para conocer cómo el módulo móvil emite los eventos de proximidad y cambios de coordenadas GPS hacia el reloj, consultar el [README del Módulo Móvil](../app/README.md).

---

### 2. `AndroidManifest.xml` (`main` Manifiesto)
* **Ubicación:** `wear/src/main/AndroidManifest.xml`
* **Propósito:** Configura los metadatos específicos del ecosistema Wearable:
  * `<uses-feature android:name="android.hardware.type.watch" />`: Declara la aplicación como exclusiva para relojes inteligentes.
  * `android.permission.WAKE_LOCK`: Permite activar la pantalla brevemente al recibir una alerta de proximidad.
  * `android.permission.VIBRATE`: Proporciona respuesta háptica al ingresar al radio de una heladería con promoción activa.
