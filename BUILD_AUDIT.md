# StepPhantom — auditoría de compilación

## Correcciones aplicadas

1. `SensorHook.kt`: se corrigió el import inexistente
   `android.app.AndroidAppHelper` por la clase real de Xposed
   `de.robv.android.xposed.AndroidAppHelper`.
2. `HealthConnectWriter.kt`: se reemplazó el constructor no público
   `Metadata()` por `Metadata.manualEntry()`.
3. Health Connect: dependencia actualizada a la versión estable
   `androidx.health.connect:connect-client:1.1.0`.
4. Android Gradle Plugin: actualizado a `8.10.1`, compatible con
   `compileSdk 36`, manteniendo Gradle `8.11.1` y Java 17 en CI.
5. Se agregó explícitamente `lifecycle-viewmodel-ktx:2.8.7` para
   `viewModelScope`.
6. GitHub Actions ahora instala explícitamente Android SDK 36 y Build Tools
   antes de ejecutar `gradle clean assembleDebug`.
7. El workflow sube reportes de Gradle cuando la compilación falla.

## Validaciones estáticas realizadas

- La entrada `assets/xposed_init` coincide con
  `com.stepphantom.xposed.SensorHook`.
- `namespace` y `applicationId` coinciden con el paquete de las clases.
- El workflow no depende de un `gradlew` incompleto: usa Gradle 8.11.1
  instalado por la acción oficial.
- El ZIP conserva `app/`, archivos Gradle y `.github/workflows/build.yml`.

## Validación pendiente

Este entorno no tiene Android SDK ni acceso de red para descargar Gradle y
las dependencias Maven, por lo que el APK no pudo compilarse localmente aquí.
La validación definitiva se realiza al subir esta carpeta al repositorio: el
workflow `Build StepPhantom APK` ejecuta la compilación y entrega el artifact
`StepPhantom-debug-apk`.
