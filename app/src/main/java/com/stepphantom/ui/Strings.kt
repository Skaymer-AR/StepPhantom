package com.stepphantom.ui

import androidx.compose.runtime.staticCompositionLocalOf

enum class Lang { ES, EN }

/** Todos los textos de la UI. Dos implementaciones (EN/ES) y cambio en vivo vía LocalStrings. */
interface Strings {
    val brand: String
    val previewLabel: String

    // Nav
    val navHome: String; val navApps: String; val navHealth: String
    val navDiag: String; val navSettings: String

    // Home
    val homeTitle: String
    val homeSubtitle: String
    fun homeSelected(apps: Int, enabled: Int): String
    val homeProfilesTitle: String
    fun homeProfilesCount(enabled: Int, selected: Int): String
    val homeTargetsTitle: String
    val homeNoTargets: String
    val homeEnabledStatus: String
    val homePausedStatus: String
    val homeDisabledStatus: String
    val homeConfigureApps: String
    fun homeMoreTargets(count: Int): String
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
    val selectedApps: String
    val availableApps: String
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
    val stepVariation: String
    val realStepsHint: String; val customPatternHint: String
    val advancedControls: String; val advancedControlsHint: String
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
    val dRoute: String; val dLastSensor: String; val dRealValue: String; val dFakeValue: String; val dSimulationStatus: String
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
    override val previewLabel = "UI 3 TEST"

    override val navHome = "Home"; override val navApps = "Apps"; override val navHealth = "Health"
    override val navDiag = "Diag"; override val navSettings = "Settings"

    override val homeTitle = "Overview"
    override val homeSubtitle = "LSPosed / Vector module · Android 16 · experimental"
    override fun homeSelected(apps: Int, enabled: Int) = "Selected apps: $apps · enabled profiles: $enabled"
    override val homeProfilesTitle = "Profiles enabled"
    override fun homeProfilesCount(enabled: Int, selected: Int) = "$enabled of $selected profiles enabled"
    override val homeTargetsTitle = "Selected test apps"
    override val homeNoTargets = "Choose a test app to create its sensor profile."
    override val homeEnabledStatus = "Profile enabled"
    override val homePausedStatus = "Paused"
    override val homeDisabledStatus = "Disabled"
    override val homeConfigureApps = "Configure apps"
    override fun homeMoreTargets(count: Int) = "+ $count more"
    override val homeRoute = "Step sensor data is supported. Health Connect is shown for information only."
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
    override val presetBody = "Choose the \"4 → 87\" example to set a target of 87 steps. You can also enter any value."

    override val appsTitle = "Apps"
    override val search = "Search apps by name"
    override val showSystem = "Include system apps"
    override val addManual = "Add app by package name"
    override val add = "Add"
    override fun copySelected(n: Int) = "Copy app package names ($n)"
    override val selectedApps = "Selected apps"
    override val availableApps = "More apps"
    override val selectedNotListed = "Selected apps outside this list"
    override val config = "Settings"
    override val systemLabel = "System app"; override val userLabel = "Installed app"

    override val back = "Back"
    override val moduleActive = "Enable for this app"
    override val hookSensorManager = "Apply to step sensor readings"
    override val mode = "Step count mode"
    override val modeOriginal = "Real steps"
    override val modeAdd = "Add steps"
    override val modeMultiply = "Scale step count"
    override val modeReplace = "Set a target count"
    override val modeRhythm = "Simulated walking pace"
    override val modeCustom = "Custom step pattern"
    override val preset487 = "Try an example: 4 → 87"
    override val params = "Step settings"
    override val offset = "Additional steps"
    override val multiplier = "Step count scale"
    override val replaceValue = "Target step count"
    override val stepsPerMinute = "Walking pace (steps/min)"
    override val initialSteps = "Starting step count"
    override fun jitter(n: Int) = "Random step variation: $n%"
    override val minLimit = "Minimum step count"
    override val maxLimit = "Maximum step count (-1 = no limit)"
    override val stepVariation = "Step count variation"
    override val realStepsHint = "Pass through the app's real step count without changes."
    override val customPatternHint = "Adjust extra steps and scale in advanced controls."
    override val advancedControls = "Advanced controls"
    override val advancedControlsHint = "Set limits, adjust variation, and configure motion sensors."
    override val pause = "Pause step updates"
    override val resetBaseline = "Restart step session"
    override val routeDiagTitle = "Health Connect checks"
    override val detectHcJetpack = "Check the Health Connect app"
    override val detectHcFramework = "Check Android Health Connect"
    override val experimentalTitle = "Extra motion sensors (experimental)"
    override val simAccel = "Simulate movement"
    override val simGyro = "Simulate rotation"

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
    override val dRealValue = "Real value"; override val dFakeValue = "Simulated value"; override val dSimulationStatus = "Synthetic event status"
    override val dUid = "UID"; override val dProcess = "Process"; override val dSdk = "SDK"
    override val dHcJetpack = "HC Jetpack"; override val dHcFramework = "HC framework"
    override val dFitRecording = "Fit/Recording"
    override val dClasses = "Classes"; override val dLastError = "Last error"; override val dUpdated = "Updated"

    override val settingsTitle = "Advanced settings"
    override val language = "Language"; override val spanish = "Español"; override val english = "English"
    override val appearance = "Appearance"
    override val dynamicColor = "Dynamic color (Material You)"
    override val dynamicColorSub = "Optional wallpaper accents on Android 12+. Off keeps the forest-green palette."
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
    override val previewLabel = "PRUEBA UI 3"

    override val navHome = "Inicio"; override val navApps = "Apps"; override val navHealth = "Health"
    override val navDiag = "Diag"; override val navSettings = "Ajustes"

    override val homeTitle = "Resumen"
    override val homeSubtitle = "Módulo LSPosed / Vector · Android 16 · experimental"
    override fun homeSelected(apps: Int, enabled: Int) = "Apps seleccionadas: $apps · perfiles habilitados: $enabled"
    override val homeProfilesTitle = "Perfiles habilitados"
    override fun homeProfilesCount(enabled: Int, selected: Int) = "$enabled de $selected perfiles habilitados"
    override val homeTargetsTitle = "Apps de prueba seleccionadas"
    override val homeNoTargets = "Elegí una app de prueba para crear su perfil de sensores."
    override val homeEnabledStatus = "Perfil habilitado"
    override val homePausedStatus = "En pausa"
    override val homeDisabledStatus = "Desactivado"
    override val homeConfigureApps = "Configurar apps"
    override fun homeMoreTargets(count: Int) = "+ $count más"
    override val homeRoute = "Se admiten datos del sensor de pasos. Health Connect se muestra sólo como información."
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
    override val presetBody = "Elegí el ejemplo \"4 → 87\" para fijar 87 pasos. También podés escribir cualquier cantidad."

    override val appsTitle = "Aplicaciones"
    override val search = "Buscar apps por nombre"
    override val showSystem = "Incluir apps del sistema"
    override val addManual = "Agregar app por nombre de paquete"
    override val add = "Agregar"
    override fun copySelected(n: Int) = "Copiar nombres de paquete ($n)"
    override val selectedApps = "Apps seleccionadas"
    override val availableApps = "Más apps"
    override val selectedNotListed = "Apps seleccionadas fuera de esta lista"
    override val config = "Ajustes"
    override val systemLabel = "App del sistema"; override val userLabel = "App instalada"

    override val back = "Volver"
    override val moduleActive = "Activar para esta app"
    override val hookSensorManager = "Aplicar a las lecturas del sensor de pasos"
    override val mode = "Forma de contar los pasos"
    override val modeOriginal = "Pasos reales"
    override val modeAdd = "Sumar pasos"
    override val modeMultiply = "Escalar cantidad de pasos"
    override val modeReplace = "Fijar una cantidad objetivo"
    override val modeRhythm = "Ritmo de caminata simulado"
    override val modeCustom = "Ritmo personalizado"
    override val preset487 = "Probar ejemplo: 4 → 87"
    override val params = "Ajustes de pasos"
    override val offset = "Pasos adicionales"
    override val multiplier = "Escala del contador"
    override val replaceValue = "Cantidad objetivo de pasos"
    override val stepsPerMinute = "Ritmo de caminata (pasos/min)"
    override val initialSteps = "Cantidad inicial de pasos"
    override fun jitter(n: Int) = "Variación aleatoria de pasos: $n%"
    override val minLimit = "Cantidad mínima de pasos"
    override val maxLimit = "Cantidad máxima de pasos (-1 = sin límite)"
    override val stepVariation = "Variación del contador de pasos"
    override val realStepsHint = "Se usa el contador real de la app sin modificarlo."
    override val customPatternHint = "Ajustá los pasos extra y la escala en Controles avanzados."
    override val advancedControls = "Controles avanzados"
    override val advancedControlsHint = "Definí límites, variación y sensores de movimiento."
    override val pause = "Pausar actualización de pasos"
    override val resetBaseline = "Reiniciar sesión de pasos"
    override val routeDiagTitle = "Comprobaciones de Health Connect"
    override val detectHcJetpack = "Comprobar la app Health Connect"
    override val detectHcFramework = "Comprobar Health Connect de Android"
    override val experimentalTitle = "Sensores de movimiento extra (experimental)"
    override val simAccel = "Simular movimiento"
    override val simGyro = "Simular rotación"

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
    override val dRealValue = "Valor real"; override val dFakeValue = "Valor simulado"; override val dSimulationStatus = "Estado del evento sintético"
    override val dUid = "UID"; override val dProcess = "Proceso"; override val dSdk = "SDK"
    override val dHcJetpack = "HC Jetpack"; override val dHcFramework = "HC framework"
    override val dFitRecording = "Fit/Recording"
    override val dClasses = "Clases"; override val dLastError = "Último error"; override val dUpdated = "Actualizado"

    override val settingsTitle = "Ajustes avanzados"
    override val language = "Idioma"; override val spanish = "Español"; override val english = "English"
    override val appearance = "Apariencia"
    override val dynamicColor = "Color dinámico (Material You)"
    override val dynamicColorSub = "Acentos opcionales del wallpaper en Android 12+. Apagado mantiene la paleta verde."
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
