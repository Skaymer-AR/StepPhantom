# StepPhantom · Módulo LSPosed / Vector (Android 16) — EXPERIMENTAL

App Android que es, a la vez, **interfaz de configuración** (Kotlin + Jetpack
Compose + Material 3) y **módulo LSPosed/Vector** que hookea `SensorManager`
para simular sensores de pasos y movimiento en paquetes que vos elijas.

Pensado para **testing local, investigación personal y depuración** sobre apps
propias o donde tengas permiso de testeo. No trae —a propósito— evasión de Play
Integrity, ocultamiento de root, anti-anti-cheat ni bypasses. Si una app valida
integridad/root/hooks, **este módulo no la esconde**: puede detectar el entorno.

> Nada de esto promete "funciona con todas las apps". Al contrario: abajo hay
> una lista honesta de todo lo que puede no andar.

---

## 1. Requisitos

- Root con **Magisk** o **KernelSU** y **Zygisk** habilitado (o NeoZygisk).
- **LSPosed** o su fork moderno **Vector** (JingMatrix) instalado como módulo Zygisk.
- Android **8.1–16** (Vector declara soporte hasta 17 beta; 16 soportado en estable).
- Android Studio reciente (para compilar el APK).

---

## 2. Qué API de Xposed usa y por qué

Uso la **API clásica de Xposed (nivel 82)**: `de.robv.android.xposed.*`
(`IXposedHookLoadPackage`, `XposedHelpers`, `XposedBridge`, `XC_MethodHook`) más
`assets/xposed_init`.

| Opción | Ventaja | Desventaja | Decisión |
|---|---|---|---|
| **API clásica 82** (`de.robv.android.xposed`) | Compatible con LSPosed **y** Vector; documentadísima; estable | Es "vieja" | ✅ Elegida |
| **libxposed API 100** (`io.github.libxposed`) | Moderna, mejor rendimiento | API 100 nunca se publicó oficialmente; sólo Vector 2.0 la "cierra" | ❌ |
| **libxposed API 101/102** | Es hacia donde va el ecosistema | Cambios *breaking* entre 100→101→102; soporte en transición | ❌ (por ahora) |

Vector mantiene consistencia de API con Xposed clásico, así que un módulo 82
carga en ambos sin cambios. Es la opción con menos superficie para romperse.

---

## 3. Árbol de archivos

```
StepPhantom/
├── settings.gradle
├── build.gradle
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── README.md
└── app/
    ├── build.gradle
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── assets/xposed_init                 # -> com.stepphantom.xposed.SensorHook
        ├── res/values/{strings,themes}.xml
        └── java/com/stepphantom/
            ├── MainActivity.kt
            ├── ui/
            │   ├── MainScreen.kt              # UI Compose completa
            │   ├── MainViewModel.kt
            │   ├── AppDiagnostics.kt          # log de config in-app
            │   └── theme/Theme.kt
            ├── config/
            │   ├── ModuleConfig.kt            # modelo + JSON
            │   ├── ConfigRepository.kt        # persistencia (lado app)
            │   ├── ConfigProvider.kt          # ContentProvider (puente IPC)
            │   └── HookConfigSource.kt        # lectura de config (lado hook)
            ├── engine/
            │   └── StepSimulationEngine.kt    # motor puro de simulación
            └── xposed/
                ├── SensorHook.kt              # entrypoint IXposedHookLoadPackage
                ├── SensorListenerWrapper.kt   # wrapper del SensorEventListener
                └── SensorEventFactory.kt      # fábrica de SensorEvent (experimental)
```

---

## 4. Cómo compilar (obtener el APK)

**Opción A — Android Studio (recomendada):**
1. `File > Open` y elegí la carpeta `StepPhantom/`. Android Studio regenera el
   Gradle wrapper y sincroniza. Si sugiere subir AGP/Kotlin para `compileSdk 36`,
   aceptá.
2. `Build > Build App Bundle(s) / APK(s) > Build APK(s)`.
3. El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

**Opción B — Línea de comandos:**
```bash
cd StepPhantom
# una sola vez, si no tenés wrapper jar:
gradle wrapper --gradle-version 8.13
./gradlew assembleDebug
# APK -> app/build/outputs/apk/debug/app-debug.apk
```

> Si `api.xposed.info` no resuelve el `de.robv.android.xposed:api:82`, bajá el
> `api-82.jar` a mano, ponelo en `app/libs/` y cambiá la dependencia por
> `compileOnly files('libs/api-82.jar')`.

---

## 5. Instalar y activar en LSPosed / Vector

1. Instalá el APK: `adb install app-debug.apk` (o desde el file manager).
2. Abrí el **manager de LSPosed/Vector** (o su "parasitic manager").
3. Pestaña **Módulos** → activá **StepPhantom**.
4. Tocá el módulo → **Scope** → marcá **sólo** las apps objetivo (ver §6).
5. **Reboot** (o soft reboot). Los hooks de framework necesitan reinicio del proceso.
6. Abrí StepPhantom, activá el switch **Módulo**, agregá los paquetes objetivo y
   configurá pasos/min, etc. La config se propaga por el ContentProvider.

---

## 6. Qué paquetes poner en scope

- En el **scope de LSPosed/Vector**: sólo la(s) app(s) de prueba. Evitá "System
  Framework" y "All apps" (riesgo de bootloop / inestabilidad).
- En **StepPhantom** (lista de paquetes objetivo): los mismos paquetes. Es un
  segundo filtro más fino. Si la lista está vacía, el módulo no simula nada.

Doble filtro a propósito: el scope decide **en qué procesos se inyecta**; la
lista de la app decide **en cuáles simula**.

---

## 7. Probar con una app de sensores simple

Podés usar cualquier lector de sensores (por ejemplo apps tipo "sensor test"), o
armar una mini-app de prueba con esto:

```kotlin
class SensorTestActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sm: SensorManager
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        // Android 10+: pedí ACTIVITY_RECOGNITION en runtime antes de registrar.
        sm = getSystemService(SENSOR_SERVICE) as SensorManager
        sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }
    override fun onSensorChanged(e: SensorEvent) {
        Log.d("SensorTest", "type=${e.sensor.type} value=${e.values[0]}")
    }
    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
}
```

Poné esa app en el scope + en la lista de StepPhantom, activá STEP_COUNTER, y
mirá cómo el valor sube según tus pasos/min configurados aunque estés quieto.

---

## 8. ¿La app lee `SensorManager` directo o usa Health Connect?

Este módulo **sólo** afecta a apps que leen sensores por `SensorManager`. Para
saber si es tu caso:

- **En el log del módulo** (§11): si aparece `Envuelto <paquete> sensor=...`,
  la app registró un listener por `SensorManager` → la estás interceptando.
  Si **nunca** aparece, no está usando esa vía.
- **Permisos del manifest** (con `aapt dump permissions` o inspección): si pide
  `com.google.android.gms.permission.ACTIVITY_RECOGNITION`, Health Connect, o usa
  la **Recording API / Fitness API** de Google Play Services, probablemente **no**
  lee `SensorManager` directo → **este módulo no la va a afectar**.
- **Decompilá** (jadx) y buscá `registerListener`, `TYPE_STEP_COUNTER`,
  `HealthConnectClient`, `RecordingClient`. Te dice la vía real en 2 minutos.

---

## 9. Cómo compartir la configuración entre app y proceso hookeado

El hook corre **dentro de la app objetivo** (otro UID). No comparte tus
SharedPreferences. Comparación de mecanismos:

| Mecanismo | ¿Anda en Android 16? | Robustez | Rol en el proyecto |
|---|---|---|---|
| `MODE_WORLD_READABLE` (clásico) | ❌ Lanza SecurityException desde Android 7 | — | Descartado |
| `XSharedPreferences` | ⚠️ Sólo con el bridge de LSPosed/Vector; puede requerir world-readable | Media/frágil | **Fallback** (`HookConfigSource`) |
| **ContentProvider exportado** | ✅ Independiente de versión | Alta | **Principal** (`ConfigProvider`) |

El hook lee con cascada: **Provider → XSharedPreferences → defaults**. El "reset"
del contador viaja como `resetToken` dentro de la misma config (no hay canal
inverso app←hook sin permisos privilegiados).

---

## 10. Viabilidad real por sensor

| Sensor | Estrategia | Viabilidad | Nota |
|---|---|---|---|
| `TYPE_STEP_COUNTER` | Scheduler propio fabrica eventos monotónicos | ✅ con salvedad | Requiere fabricar `SensorEvent` (experimental) si el device está quieto |
| `TYPE_STEP_DETECTOR` | Scheduler emite eventos `1.0f` a cadencia | ✅ con salvedad | Misma dependencia de fabricar `SensorEvent` |
| `TYPE_ACCELEROMETER` | Modifica `values[]` del evento real | ✅ estable | Sólo hay datos mientras el sensor real dispara |
| `TYPE_GYROSCOPE` | Modifica `values[]` del evento real | ✅ estable | Igual que acelerómetro |

**El punto delicado**: contador y detector necesitan *inyectar* eventos aunque no
haya movimiento real, y en AOSP `SensorEvent(int)` es **package-private**. Lo
construyo por reflexión (`SensorEventFactory`). Eso es API oculta: bajo
LSPosed/Vector el proceso suele quedar exento de la lista negra de hidden-API,
pero **no está garantizado en toda ROM**. Si falla, el wrapper cae a reutilizar
un evento real cacheado, y si tampoco hay, no entrega evento (y lo dice en el log).

---

## 11. Leer logs

- **Manager de LSPosed/Vector**: pestaña de **Logs** (module log). Ahí ves las
  líneas de `XposedBridge.log` con prefijo `[StepPhantom]`.
- **logcat**:
  ```bash
  adb logcat | grep "\[StepPhantom\]"
  ```
- El **log de configuración** in-app (pantalla Diagnóstico) muestra sólo cambios
  de config del lado app. El **runtime del hook** (último paquete/sensor/errores)
  está del lado del proceso objetivo → miralo por LSPosed/logcat.

---

## 12. Limitaciones reales en Android 16 (leer sí o sí)

- **Fabricar `SensorEvent` es experimental** (hidden-API, §10). Es lo primero que
  puede romperse entre versiones/ROMs.
- **Sensores en background**: desde hace varias versiones, apps sin foreground
  service dejan de recibir sensores en segundo plano. Si tu app de prueba se va a
  background, el flujo (real o simulado vía scheduler) puede cortarse.
- **`ACTIVITY_RECOGNITION`**: leer STEP_COUNTER/DETECTOR requiere ese permiso
  (runtime desde Android 10). Si la app objetivo no lo tiene concedido, no lee
  pasos —ni reales ni simulados.
- **Muestreo alto**: >200 Hz requiere `HIGH_SAMPLING_RATE_SENSORS`.
- **Apps que NO usan `SensorManager`**: si usan **Health Connect**, **Recording
  API** o **Fitness API** de Google Play Services, **este módulo no las afecta**
  (§8). No es una vía de sensores; es un store de datos aparte.
- **Modificar acelerómetro/giroscopio puede romper apps** que asumen física real
  (juegos, AR, brújula). Por eso vienen apagados por defecto y con warning.
- **Multi-listener**: manejo refcount por listener y evito doble-wrap por la
  delegación interna de los overloads, pero un patrón de registro/desregistro muy
  exótico podría dejar un wrapper colgado hasta el próximo unregister total.
- **Coherencia temporal**: uso `SystemClock.elapsedRealtimeNanos()` (monotónico).
  El contador **nunca decrece**; para "bajarlo" está el botón **Reset**.
- **Detección de entorno**: sin bypasses, una app con anti-tamper puede notar
  LSPosed/root/hooks. Es esperable y no lo ocultamos.

---

## 13. Qué puede fallar según fabricante / ROM

- **Nombres de campos internos**: leo el `Context` del `SystemSensorManager` vía
  campo `mContext`. Si una ROM lo renombró, caigo a `AndroidAppHelper`, pero en
  casos raros podría no haber Context temprano.
- **HAL/So del fabricante**: algunas ROMs (Samsung/Xiaomi/etc.) tienen capas
  propias de "sensor hub"/pedómetro. El overload público que hookeo debería
  seguir siendo el punto de entrada, pero no lo garantizo en todas.
- **Hidden-API más estricta**: si la reflexión sobre `SensorEvent` se bloquea,
  agregá `org.lsposed.hiddenapibypass:hiddenapibypass` y envolvé la construcción.
- **GrapheneOS y similares**: endurecimientos extra pueden afectar tanto a
  Zygisk como a la reflexión.

---

## Estado

Base **funcional pero experimental**. Compila a APK, se instala, se activa como
módulo y hookea sensores en los paquetes elegidos. Es un esqueleto sólido para
iterar, no un producto pulido: esperá ajustes finos al abrirlo en Android Studio.
