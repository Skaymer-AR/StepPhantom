package com.stepphantom.ui

import androidx.compose.runtime.staticCompositionLocalOf

enum class Lang { ES, EN }

/** Todos los textos de la UI. Dos implementaciones (EN/ES) y cambio en vivo vía LocalStrings. */
interface Strings {
    val brand: String

    // Nav
    val navHome: String; val navApps: String; val navHealth: String
    val navDiag: String; val navSettings: String

    // Home
    val homeSubtitle: String
    fun homeSelected(apps: Int, enabled: Int): String
    val homeRoute: String
    val scopeWarning: String
    val howToActivate: String
    val steps: List<String>
    val presetTitle: String
    val presetBody: String

    // Apps
    val appsTitle: String
    val search: String
    val showSystem: String
    val addManual: String
    val add: String
    fun copySelected(n: Int): String
    val selectedNotListed: String
    val config: String
    val systemLabel: String
    val userLabel: String

    // Package config
    val back: String
    val moduleActive: String
    val hookSensorManager: String
    val mode: String
    val modeOriginal: String; val modeAdd: String; val modeMultiply: String
    val modeReplace: String; val modeRhythm: String; val modeCustom: String
    val preset487: String
    val params: String
    val offset: String; val multiplier: String; val replaceValue: String
    val stepsPerMinute: String; val initialSteps: String
    fun jitter(n: Int): String
    val minLimit: String; val maxLimit: String
    val pause: String; val resetBaseline: String
    val routeDiagTitle: String; val detectHcJetpack: String; val detectHcFramework: String
    val experimentalTitle: String; val simAccel: String; val simGyro: String

    // Health
    val healthTitle: String
    val healthWarn: String
    val status: String
    val hcAvailableLabel: String
    val permsGrantedLabel: String
    val hcInstallHint: String
    val grantPerms: String
    val writeTestTitle: String
    val stepCount: String
    val write: String; val deleteMine: String
    val noteTitle: String; val healthNote: String

    // Diagnostics
    val diagTitle: String
    val clear: String
    val diagIntro: String
    val diagEmpty: String
    val dRoute: String; val dLastSensor: String; val dRealValue: String; val dFakeValue: String
    val dUid: String; val dProcess: String; val dSdk: String
    val dHcJetpack: String; val dHcFramework: String; val dFitRecording: String
    val dClasses: String; val dLastError: String; val dUpdated: String

    // Settings
    val settingsTitle: String
    val language: String; val spanish: String; val english: String
    val appearance: String; val dynamicColor: String; val dynamicColorSub: String
    val jsonConfig: String; val export: String; val importLabel: String
    val killSwitch: String; val killSwitchBody: String; val create: String; val delete: String
    val copyCreateCmd: String
    val healthSeparateWarn: String
    val configLog: String; val noEvents: String

    // Dialogs / common
    val exportTitle: String; val importTitle: String
    val close: String; val cancel: String; val pasteJson: String; val invalidJson: String
    val yes: String; val no: String
    fun yesNo(b: Boolean): String = if (b) yes else no
}

object EnStrings : Strings {
    override val brand = "StepPhantom"

    override val navHome = "Home"; override val navApps = "Apps"; override val navHealth = "Health"
    override val navDiag = "Diag"; override val navSettings = "Settings"

    override val homeSubtitle = "LSPosed / Vector module · Android 16 · experimental"
    override fun homeSelected(apps: Int, enabled: Int) = "Selected apps: $apps · module active: $enabled"
    override val homeRoute = "Implemented route: SensorManager. Health Connect: diagnostics only (not rewritten)."
    override val scopeWarning = "You also have to select these apps in the LSPosed / Vector Scope. " +
        "StepPhantom cannot change the Scope automatically with the classic API."
    override val howToActivate = "How to activate"
    override val steps = listOf(
        "1. Pick the app in the Apps tab and configure it.",
        "2. Open LSPosed / Vector.",
        "3. Enable StepPhantom under Modules.",
        "4. Open the module's Scope.",
        "5. Check the SAME app.",
        "6. Force-stop or restart the target app's process."
    )
    override val presetTitle = "Demo preset"
    override val presetBody = "In an app's config there's a \"4 → 87\" button: sets REPLACE mode with value 87 " +
        "(demo only, via SensorManager). You can also type any value."

    override val appsTitle = "Applications"
    override val search = "Search by name or package"
    override val showSystem = "Show system apps"
    override val addManual = "Add package manually"
    override val add = "Add"
    override fun copySelected(n: Int) = "Copy selected ($n)"
    override val selectedNotListed = "Selected, not listed"
    override val config = "Config"
    override val systemLabel = "system"; override val userLabel = "user"

    override val back = "Back"
    override val moduleActive = "Module active for this app"
    override val hookSensorManager = "Hook SensorManager (implemented route)"
    override val mode = "Mode"
    override val modeOriginal = "ORIGINAL (unchanged)"
    override val modeAdd = "ADD (real + offset)"
    override val modeMultiply = "MULTIPLY (real × mult)"
    override val modeReplace = "REPLACE (target value)"
    override val modeRhythm = "SIMULATED PACE (time-based)"
    override val modeCustom = "CUSTOM (combined)"
    override val preset487 = "Preset 4 → 87"
    override val params = "Parameters"
    override val offset = "Offset (ADD)"
    override val multiplier = "Multiplier (MULTIPLY)"
    override val replaceValue = "Target value (REPLACE)"
    override val stepsPerMinute = "Steps per minute"
    override val initialSteps = "Initial steps"
    override fun jitter(n: Int) = "Jitter: $n%"
    override val minLimit = "Min limit (CUSTOM)"
    override val maxLimit = "Max limit (-1 = none)"
    override val pause = "Pause"
    override val resetBaseline = "Reset baseline / session"
    override val routeDiagTitle = "Route diagnostics (does not rewrite)"
    override val detectHcJetpack = "Detect Health Connect Jetpack"
    override val detectHcFramework = "Detect Health Connect framework"
    override val experimentalTitle = "Experimental (may break apps)"
    override val simAccel = "Simulate accelerometer"
    override val simGyro = "Simulate gyroscope"

    override val healthTitle = "Health Connect"
    override val healthWarn = "This is NOT the module. The LSPosed module alters what a selected app SEES " +
        "(it intercepts sensors). This screen WRITES real test data into the Health Connect repository via " +
        "the official API, attributed to StepPhantom, without hiding the origin. They are not equivalent."
    override val status = "Status"
    override val hcAvailableLabel = "Health Connect available"
    override val permsGrantedLabel = "Step permissions granted"
    override val hcInstallHint = "Install/enable Health Connect (on Android 14+ it ships with the system) and come back."
    override val grantPerms = "Grant permissions (WRITE/READ steps)"
    override val writeTestTitle = "Write test data"
    override val stepCount = "Step count"
    override val write = "Write"; override val deleteMine = "Delete mine"
    override val noteTitle = "Note"
    override val healthNote = "Delete only removes StepsRecords whose DataOrigin is StepPhantom. It doesn't touch " +
        "other sources or try to hide anything. Off by default: nothing is written without your action."

    override val diagTitle = "Diagnostics"
    override val clear = "Clear"
    override val diagIntro = "What the hook reports from inside each selected app's process. If an app isn't here, " +
        "it hasn't loaded with the module yet (check Scope + restart its process). Raw detail is also in the " +
        "LSPosed/Vector log and logcat (filter \"[StepPhantom]\")."
    override val diagEmpty = "No diagnostics yet."
    override val dRoute = "Detected route"; override val dLastSensor = "Last sensor"
    override val dRealValue = "Real value"; override val dFakeValue = "Simulated value"
    override val dUid = "UID"; override val dProcess = "Process"; override val dSdk = "SDK"
    override val dHcJetpack = "HC Jetpack"; override val dHcFramework = "HC framework"
    override val dFitRecording = "Fit/Recording"
    override val dClasses = "Classes"; override val dLastError = "Last error"; override val dUpdated = "Updated"

    override val settingsTitle = "Advanced settings"
    override val language = "Language"; override val spanish = "Español"; override val english = "English"
    override val appearance = "Appearance"
    override val dynamicColor = "Dynamic color (Material You)"
    override val dynamicColorSub = "Uses your wallpaper colors on Android 12+. Off = built-in expressive palette."
    override val jsonConfig = "JSON configuration"
    override val export = "Export"; override val importLabel = "Import"
    override val killSwitch = "Emergency kill switch"
    override val killSwitchBody = "If the module causes trouble, create this file via ADB/root and restart the " +
        "target process. With the file present, the module installs no hooks."
    override val create = "Create:"; override val delete = "Delete:"
    override val copyCreateCmd = "Copy create command"
    override val healthSeparateWarn = "Official Health Connect writing lives in the Health tab, is off by default, " +
        "and is NOT equivalent to the module: it adds real data, it doesn't alter what an app sees."
    override val configLog = "Configuration log"; override val noEvents = "No events."

    override val exportTitle = "Configuration (JSON)"; override val importTitle = "Import JSON"
    override val close = "Close"; override val cancel = "Cancel"
    override val pasteJson = "Paste the JSON here"; override val invalidJson = "Invalid JSON."
    override val yes = "yes"; override val no = "no"
}

object EsStrings : Strings {
    override val brand = "StepPhantom"

    override val navHome = "Inicio"; override val navApps = "Apps"; override val navHealth = "Health"
    override val navDiag = "Diag"; override val navSettings = "Ajustes"

    override val homeSubtitle = "Módulo LSPosed / Vector · Android 16 · experimental"
    override fun homeSelected(apps: Int, enabled: Int) = "Apps seleccionadas: $apps · con módulo activo: $enabled"
    override val homeRoute = "Ruta implementada: SensorManager. Health Connect: sólo diagnóstico (no se reescribe)."
    override val scopeWarning = "También tenés que seleccionar estas apps en el Scope de LSPosed / Vector. " +
        "StepPhantom NO puede cambiar el Scope automáticamente con la API clásica."
    override val howToActivate = "Cómo activar"
    override val steps = listOf(
        "1. Seleccioná la app en la pestaña Apps y configurala.",
        "2. Abrí LSPosed / Vector.",
        "3. Activá StepPhantom en Módulos.",
        "4. Entrá en Scope del módulo.",
        "5. Marcá la MISMA app.",
        "6. Forzá detención o reiniciá el proceso de la app objetivo."
    )
    override val presetTitle = "Preset de demo"
    override val presetBody = "En la config de una app tenés el botón \"4 → 87\": pone modo REEMPLAZAR con " +
        "valor 87 (sólo demostración por SensorManager). También podés escribir cualquier valor."

    override val appsTitle = "Aplicaciones"
    override val search = "Buscar por nombre o package"
    override val showSystem = "Mostrar apps del sistema"
    override val addManual = "Agregar package manual"
    override val add = "Agregar"
    override fun copySelected(n: Int) = "Copiar seleccionadas ($n)"
    override val selectedNotListed = "Seleccionadas no listadas"
    override val config = "Config"
    override val systemLabel = "sistema"; override val userLabel = "usuario"

    override val back = "Volver"
    override val moduleActive = "Módulo activo para esta app"
    override val hookSensorManager = "Hook SensorManager (ruta implementada)"
    override val mode = "Modo"
    override val modeOriginal = "ORIGINAL (sin cambios)"
    override val modeAdd = "SUMAR (real + offset)"
    override val modeMultiply = "MULTIPLICAR (real × mult)"
    override val modeReplace = "REEMPLAZAR (valor objetivo)"
    override val modeRhythm = "RITMO_SIMULADO (por tiempo)"
    override val modeCustom = "PERSONALIZADO (combinado)"
    override val preset487 = "Preset 4 → 87"
    override val params = "Parámetros"
    override val offset = "Offset (SUMAR)"
    override val multiplier = "Multiplicador (MULTIPLICAR)"
    override val replaceValue = "Valor objetivo (REEMPLAZAR)"
    override val stepsPerMinute = "Pasos por minuto"
    override val initialSteps = "Pasos iniciales"
    override fun jitter(n: Int) = "Jitter: $n%"
    override val minLimit = "Límite mínimo (PERSONALIZADO)"
    override val maxLimit = "Límite máximo (-1 = sin límite)"
    override val pause = "Pausa"
    override val resetBaseline = "Reiniciar baseline / sesión"
    override val routeDiagTitle = "Diagnóstico de vías (no reescribe)"
    override val detectHcJetpack = "Detectar Health Connect Jetpack"
    override val detectHcFramework = "Detectar Health Connect framework"
    override val experimentalTitle = "Experimental (puede romper apps)"
    override val simAccel = "Simular acelerómetro"
    override val simGyro = "Simular giroscopio"

    override val healthTitle = "Health Connect"
    override val healthWarn = "Esto NO es el módulo. El módulo LSPosed altera lo que VE una app seleccionada " +
        "(intercepta sensores). Esta pantalla ESCRIBE datos reales de prueba en el repositorio de Health Connect " +
        "usando la API oficial, atribuidos a StepPhantom, sin ocultar el origen. No son equivalentes."
    override val status = "Estado"
    override val hcAvailableLabel = "Health Connect disponible"
    override val permsGrantedLabel = "Permisos de pasos concedidos"
    override val hcInstallHint = "Instalá/activá Health Connect (en Android 14+ viene en el sistema) y volvé."
    override val grantPerms = "Conceder permisos (WRITE/READ pasos)"
    override val writeTestTitle = "Escribir datos de prueba"
    override val stepCount = "Cantidad de pasos"
    override val write = "Escribir"; override val deleteMine = "Borrar lo mío"
    override val noteTitle = "Nota"
    override val healthNote = "El borrado sólo elimina StepsRecords cuyo DataOrigin es StepPhantom. No toca datos " +
        "de otras fuentes ni intenta ocultar nada. Apagado por defecto: no se escribe nada sin tu acción."

    override val diagTitle = "Diagnóstico"
    override val clear = "Limpiar"
    override val diagIntro = "Lo que reporta el hook desde el proceso de cada app seleccionada. Si una app no aparece, " +
        "todavía no se cargó con el módulo (revisá Scope + reiniciá su proceso). El detalle crudo también está en el " +
        "log de LSPosed/Vector y en logcat (filtrá \"[StepPhantom]\")."
    override val diagEmpty = "Sin diagnósticos todavía."
    override val dRoute = "Vía detectada"; override val dLastSensor = "Último sensor"
    override val dRealValue = "Valor real"; override val dFakeValue = "Valor simulado"
    override val dUid = "UID"; override val dProcess = "Proceso"; override val dSdk = "SDK"
    override val dHcJetpack = "HC Jetpack"; override val dHcFramework = "HC framework"
    override val dFitRecording = "Fit/Recording"
    override val dClasses = "Clases"; override val dLastError = "Último error"; override val dUpdated = "Actualizado"

    override val settingsTitle = "Ajustes avanzados"
    override val language = "Idioma"; override val spanish = "Español"; override val english = "English"
    override val appearance = "Apariencia"
    override val dynamicColor = "Color dinámico (Material You)"
    override val dynamicColorSub = "Usa los colores de tu wallpaper en Android 12+. Apagado = paleta expresiva propia."
    override val jsonConfig = "Configuración JSON"
    override val export = "Exportar"; override val importLabel = "Importar"
    override val killSwitch = "Kill switch de emergencia"
    override val killSwitchBody = "Si el módulo causa problemas, creá este archivo por ADB/root y reiniciá el " +
        "proceso objetivo. Con el archivo presente, el módulo NO instala ningún hook."
    override val create = "Crear:"; override val delete = "Borrar:"
    override val copyCreateCmd = "Copiar comando crear"
    override val healthSeparateWarn = "La escritura oficial en Health Connect vive en la pestaña Health, está apagada " +
        "por defecto y NO es equivalente al módulo: agrega datos reales, no altera lo que ve una app."
    override val configLog = "Log de configuración"; override val noEvents = "Sin eventos."

    override val exportTitle = "Configuración (JSON)"; override val importTitle = "Importar JSON"
    override val close = "Cerrar"; override val cancel = "Cancelar"
    override val pasteJson = "Pegá el JSON acá"; override val invalidJson = "JSON inválido."
    override val yes = "sí"; override val no = "no"
}

fun stringsFor(lang: Lang): Strings = if (lang == Lang.EN) EnStrings else EsStrings

val LocalStrings = staticCompositionLocalOf<Strings> { EsStrings }
