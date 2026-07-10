# Xposed carga la clase del modulo por NOMBRE (assets/xposed_init). No la toques.
-keep class com.stepphantom.xposed.** { *; }
-keep class * implements de.robv.android.xposed.IXposedHookLoadPackage { *; }

# Providers referenciados por nombre desde el manifest.
-keep class com.stepphantom.config.ConfigProvider { *; }
-keep class com.stepphantom.config.DiagnosticsProvider { *; }

# API Xposed es compileOnly; no deberia empaquetarse.
-dontwarn de.robv.android.xposed.**

# Health Connect
-dontwarn androidx.health.connect.**
