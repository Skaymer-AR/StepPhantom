package com.stepphantom.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

data class AppInfo(val packageName:String,val label:String,val isSystem:Boolean,val icon:Drawable?)
object AppEnumerator {
    fun list(context:Context,includeSystem:Boolean):List<AppInfo>{ val pm=context.packageManager; return pm.getInstalledApplications(0).asSequence().filter{includeSystem||(it.flags and ApplicationInfo.FLAG_SYSTEM)==0}.map{ AppInfo(it.packageName,runCatching{pm.getApplicationLabel(it).toString()}.getOrDefault(it.packageName),(it.flags and ApplicationInfo.FLAG_SYSTEM)!=0,runCatching{pm.getApplicationIcon(it)}.getOrNull()) }.sortedBy{it.label.lowercase()}.toList() }
    fun exists(context:Context,packageName:String):Boolean=runCatching{context.packageManager.getApplicationInfo(packageName,0);true}.getOrDefault(false)
}
