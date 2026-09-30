package ru.professionals.combats.platform

import android.util.Log
import io.appmetrica.analytics.AppMetrica
import ru.professionals.combats.BuildConfig

/** Records event names only, never credentials or personal data. Created 30-09-2026; author: training project. */
object Analytics {
    fun event(name: String) {
        Log.i("Analytics", "[Analytics]: Event — $name")
        if (BuildConfig.APPMETRICA_API_KEY.isNotBlank()) AppMetrica.reportEvent(name)
    }
}
