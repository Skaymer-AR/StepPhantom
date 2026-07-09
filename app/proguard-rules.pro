# Xposed carga la clase del módulo por NOMBRE (desde assets/xposed_init).
# Si R8/ProGuard la renombra o la elimina, el módulo no carga. Mantenela.
-keep class com.stepphantom.xposed.** { *; }
-keep class * implements de.robv.android.xposed.IXposedHookLoadPackage { *; }

# El ContentProvider también se referencia por nombre desde el manifest.
-keep class com.stepphantom.config.ConfigProvider { *; }

# La API de Xposed es compileOnly; nunca debería empaquetarse, pero por las dudas:
-dontwarn de.robv.android.xposed.**
