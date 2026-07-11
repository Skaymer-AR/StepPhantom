# StepPhantom · versión recortada (LSPosed / Vector · Android 16)

App Android + módulo LSPosed/Vector para **testing local y apps propias o con
autorización**. Transforma pasos **por aplicación** interceptando `SensorManager`,
detecta por qué vía lee pasos cada app (diagnóstico), y trae un **writer oficial**
de datos de prueba para Health Connect (separado, apagado por defecto).


## Novedades de esta versión (UI)

- **Material You expresivo**: paleta violeta/turquesa/naranja, esquinas redondeadas,
  tipografía con titulares grandes, tarjetas con contenedores de color.
- **Color dinámico** del wallpaper en Android 12+ (toggle en Ajustes; apagado usa la
  paleta propia). Funciona en claro y oscuro.
- **Idioma ES/EN** con selector adentro de la app (botón en la barra superior y en
  Ajustes). Cambia en vivo, sin reiniciar la actividad.
- **Ícono propio** (adaptive icon "fantasma" con capa monocromática para el ícono
  temático de Android 13+).
- **Transiciones animadas** entre pantallas (fade + slide) y tarjetas con
  `animateContentSize`.

La lógica (config por app, motor de transformación, hooks, writer de HC) no cambió.

## Qué hace y qué NO hace

| Capacidad | Estado |
|---|---|
| Transformar `TYPE_STEP_COUNTER` por app vía `SensorManager` | ✅ implementado |
| `TYPE_STEP_DETECTOR` (passthrough + sintético en RITMO) | ✅ implementado |
| Acelerómetro/giroscopio | 🧪 experimental, apagado por defecto |
| Selector de apps (icono/nombre/package, búsqueda, sistema, manual, copiar) | ✅ |
| Config por paquete + 6 modos + monotonicidad + baseline | ✅ |
| **Detección** de Health Connect Jetpack / framework / Fit-Recording | ✅ (sólo informa) |
| **Escritura oficial** de prueba en Health Connect (WRITE_STEPS) | ✅ (apagado por defecto) |
| Diagnóstico por app con canal inverso desde el hook | ✅ |
| Kill switch por archivo, ContentProvider endurecido, CI en Actions | ✅ |
| **Reescribir lecturas de Health Connect** (`readRecords`/`aggregate`) | ❌ fuera de esta versión |
| Reescribir Google Fit / Recording API | ❌ fuera de esta versión |
| Bypass de Play Integrity / root hiding / anti-anti-cheat / anti-LSPosed | ❌ no incluido |

**Por qué queda afuera la reescritura de lecturas de Health Connect:** interceptar
`readRecords`/`aggregate` para devolverle a una app de terceros un total que no
generaste, preservando `DataOrigin` y estabilidad para que "no se note", es
fabricar actividad creíble ante un lector externo. Para testear tu propia app que
lee de HC, la vía correcta es **escribir StepsRecords de prueba con la API oficial**
(está incluido, pestaña Health). La detección de esas vías sí quedó, para que sepas
si el hook de sensores va a servir o no en cada app.

## Arquitectura (3 capas)

- **Capa 1 — Selector** (`ui/AppsScreen.kt`, `ui/AppInfo.kt`): lista apps con
  `QUERY_ALL_PACKAGES`, búsqueda, filtro sistema, alta manual con validación,
  copiar selección, config separada por paquete. Deja claro que **también** hay
  que marcar la app en el Scope de LSPosed/Vector (la API clásica no cambia scope).
- **Capa 2 — Motor** (`engine/StepTransformationEngine.kt`): 6 modos (ORIGINAL,
  SUMAR, MULTIPLICAR, REEMPLAZAR, RITMO_SIMULADO, PERSONALIZADO), contador
  monotónico, baseline real + simulado, sin doble aplicación, sin negativos, sin
  overflow, coherencia con `elapsedRealtimeNanos`.
- **Capa 3 — Hooks** (`xposed/`): `SensorHook` (entry `IXposedHookLoadPackage`),
  `SensorListenerWrapper`, `ListenerRegistry`, `RouteDetector`, `SensorEventFactory`.

## Árbol de archivos

```
StepPhantom/
├── settings.gradle · build.gradle · gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── .github/workflows/build.yml
└── app/
    ├── build.gradle · proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── assets/xposed_init            # com.stepphantom.xposed.SensorHook
        ├── res/values/{strings,themes}.xml
        └── java/com/stepphantom/
            ├── MainActivity.kt
            ├── config/  Config.kt, ConfigRepository.kt, ConfigProvider.kt,
            │            HookConfigSource.kt, DiagnosticsProvider.kt, DiagnosticsStore.kt
            ├── engine/  StepTransformationEngine.kt
            ├── health/  HealthConnectWriter.kt
            ├── xposed/  SensorHook.kt, SensorListenerWrapper.kt, ListenerRegistry.kt,
            │            RouteDetector.kt, SensorEventFactory.kt
            └── ui/      MainScreen.kt, AppsScreen.kt, HealthConnectScreen.kt,
                         DiagnosticsScreen.kt, MainViewModel.kt, AppInfo.kt,
                         AppDiagnostics.kt, theme/Theme.kt
```

## Compilar

**GitHub Actions** (ya incluido en `.github/workflows/build.yml`): Java 17, Android
SDK, Gradle 8.11.1, `gradle assembleDebug`, sube el APK como artifact
`StepPhantom-debug-apk`. Usa `gradle` (no `./gradlew`) para no depender de un
wrapper jar en el repo.

**Local con Android Studio:** abrí la carpeta; Studio regenera el wrapper y
sincroniza. Si sugiere subir AGP/Kotlin para `compileSdk 36`, aceptá. Luego
*Build → Build APK(s)*. Sale en `app/build/outputs/apk/debug/app-debug.apk`.

**Local por CLI** (si no tenés wrapper jar):
```bash
gradle wrapper --gradle-version 8.11.1
./gradlew assembleDebug
```

> Si `api.xposed.info` no resuelve `de.robv.android.xposed:api:82`, bajá el
> `api-82.jar`, ponelo en `app/libs/` y usá `compileOnly files('libs/api-82.jar')`.

## Reemplazar la carpeta en tu repo y pushear

```bash
# desde la raíz de tu repo clonado:
rm -rf app .github/workflows/build.yml settings.gradle build.gradle gradle.properties
cp -r /ruta/a/StepPhantom/* .        # copiá el contenido de esta carpeta encima
git add -A
git commit -m "StepPhantom recortado: transform por app (SensorManager), diagnóstico HC, writer oficial, CI"
git push origin main
```
(Si tu rama es `master`, cambiá `main` por `master`.)

## Activar en LSPosed / Vector

1. Instalá el APK (`adb install app-debug.apk`).
2. Abrí StepPhantom → pestaña **Apps** → seleccioná la app → **Config** → activá el
   módulo para esa app y elegí el modo (probá el preset **4 → 87**).
3. Abrí LSPosed/Vector → **Módulos** → activá StepPhantom.
4. Entrá en **Scope** del módulo → marcá la **misma** app.
5. **Forzá detención** o reiniciá el proceso de la app objetivo (o reboot).

## Scope: qué marcar

Sólo tus apps de prueba. Evitá **System Framework** y **All apps** (riesgo de
bootloop). Doble filtro: el Scope decide **en qué procesos** se inyecta; la config
de StepPhantom decide **en cuáles** transforma.

## Probar SensorManager

Mini-app de prueba (o cualquier lector de sensores):
```kotlin
class SensorTestActivity : ComponentActivity(), SensorEventListener {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val sm = getSystemService(SENSOR_SERVICE) as SensorManager
        // Android 10+: pedí ACTIVITY_RECOGNITION en runtime antes de registrar.
        sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }
    override fun onSensorChanged(e: SensorEvent) =
        Log.d("SensorTest", "counter=${e.values[0]}")
    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
}
```
Ponela en Scope + en la lista, activá modo REEMPLAZAR=87 y deberías ver 87 aunque
estés quieto (el scheduler empuja el valor; ver limitaciones sobre fabricar eventos).

## Probar Health Connect (writer oficial)

Pestaña **Health**: si HC está disponible, concedé permisos, escribí N pasos de
prueba (quedan atribuidos a StepPhantom) y podés borrar **sólo lo tuyo**. Esto
agrega datos reales al repositorio; **no** es lo que hace el módulo con las apps.

## Leer logs

- **LSPosed/Vector** → pestaña de logs del módulo: líneas con prefijo `[StepPhantom]`.
- **logcat**: `adb logcat | grep "\[StepPhantom\]"`.
- **Pantalla Diagnóstico** (in-app): muestra, por app, la vía detectada, valor real
  vs simulado, UID/proceso/SDK, clases encontradas y errores — vía el canal inverso
  (`DiagnosticsProvider`). El log crudo completo está en LSPosed/logcat.

## Desactivar en emergencia (kill switch)

```bash
adb shell "touch /data/local/tmp/stepphantom_disable"   # desactiva: no instala hooks
# reiniciá el proceso objetivo (o reboot)
adb shell "rm /data/local/tmp/stepphantom_disable"       # reactiva
```

## Limitaciones reales (Android 16)

- **Fabricar `SensorEvent` es experimental** (constructor package-private, hidden-API).
  Bajo LSPosed/Vector el proceso suele quedar exento, pero no en toda ROM. Si falla,
  el wrapper reutiliza un evento real cacheado; si no hay, no empuja (lo dice el log).
  Los modos SUMAR/MULTIPLICAR/PERSONALIZADO transforman el evento real **sin** fabricar,
  así que son los más estables.
- **Apps que no usan `SensorManager`** (Health Connect, Recording/Fitness API) **no se
  ven afectadas** por el módulo. El diagnóstico te avisa cuál vía usan.
- **Sensores en background**: apps sin foreground service pueden dejar de recibir eventos.
- **`ACTIVITY_RECOGNITION`**: la app objetivo necesita ese permiso para leer pasos.
- **Health Connect / SPN 2026**: HC puede tomar pasos del teléfono y atribuirlos a un
  Synthetic Package Name por app. Como este build **no** reescribe lecturas de HC, esa
  atribución no la tocamos; el diagnóstico reporta lo que ve la detección.
- **Acelerómetro/giroscopio** pueden romper apps: por eso están apagados por defecto.
- **Detección de entorno**: sin bypasses, una app con anti-tamper puede notar root/hooks.

## Versiones que quizá tengas que ajustar

- **AGP/Gradle/compileSdk 36**: fijé AGP 8.9.1 + Gradle 8.11.1 + Kotlin 2.1.0. Si tu
  entorno pide otra combinación para `compileSdk 36`, seguí la sugerencia de Studio
  (el workflow ya usa Gradle 8.11.1).
- **Health Connect** (`connect-client:1.1.0-alpha07`): si subís la versión, revisá la
  API de `Metadata` y `getGrantedPermissions()` en `HealthConnectWriter.kt`.

## Estado

Base **experimental** y coherente para la ruta de sensores. Compila a APK (local o
por Actions). Esperá ajustes finos de versiones al abrirlo en Studio.
