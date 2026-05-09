package com.example.volimiter

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconManager {

    private const val ALIAS = "com.example.volimiter.MainActivityLauncher"

    fun hideIcon(context: Context) {
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, ALIAS),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    fun showIcon(context: Context) {
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, ALIAS),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    fun isIconHidden(context: Context): Boolean {
        val state = context.packageManager.getComponentEnabledSetting(
            ComponentName(context, ALIAS)
        )
        return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }
}