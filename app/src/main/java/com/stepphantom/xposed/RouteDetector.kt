package com.stepphantom.xposed

object RouteDetector {
    data class Result(val sensorManagerAvailable:Boolean,val hcJetpack:Boolean,val hcFramework:Boolean,val fitOrRecording:Boolean,val classesFound:List<String>) {
        fun primaryRouteLabel(): String = when {
            hcJetpack -> "Health Connect (Jetpack) — este módulo NO la reescribe"
            fitOrRecording -> "Google Fit / Recording API — este módulo NO la reescribe"
            hcFramework -> "Health Connect (framework) — disponible en el SO; verificá si la app la usa"
            sensorManagerAvailable -> "SensorManager (interceptable por este módulo)"
            else -> "desconocida"
        }
    }
    private val HC_JETPACK=listOf("androidx.health.connect.client.HealthConnectClient","androidx.health.connect.client.records.StepsRecord")
    private val HC_FRAMEWORK=listOf("android.health.connect.HealthConnectManager","android.health.connect.datatypes.StepsRecord")
    private val FIT=listOf("com.google.android.gms.fitness.FitnessOptions","com.google.android.gms.fitness.RecordingClient")
    fun detect(classLoader:ClassLoader):Result { val found=mutableListOf<String>(); val j=anyLoadable(classLoader,HC_JETPACK,found); val f=anyLoadable(classLoader,HC_FRAMEWORK,found); val fit=anyLoadable(classLoader,FIT,found); val sm=isLoadable(classLoader,"android.hardware.SensorManager",found); return Result(sm,j,f,fit,found) }
    private fun anyLoadable(cl:ClassLoader,names:List<String>,acc:MutableList<String>):Boolean { var any=false; for(n in names) if(isLoadable(cl,n,acc)) any=true; return any }
    private fun isLoadable(cl:ClassLoader,name:String,acc:MutableList<String>):Boolean = try { cl.loadClass(name); acc.add(name); true } catch(_:Throwable){ false }
}
