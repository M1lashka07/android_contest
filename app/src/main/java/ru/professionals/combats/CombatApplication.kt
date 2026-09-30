package ru.professionals.combats

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import ru.professionals.combats.data.SupabaseCombatRepository

/** Initializes cached media and optional analytics. Created 30-09-2026; author: training project. */
class CombatApplication : Application(), ImageLoaderFactory {
    // One session controller across activity recreation and notification launches.
    val repository by lazy { SupabaseCombatRepository(this, BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) }
    override fun onCreate() {
        super.onCreate()
        Log.i("CombatApplication", "[CombatApplication]: Lifecycle — Created")
        if (BuildConfig.APPMETRICA_API_KEY.isNotBlank()) {
            AppMetrica.activate(this, AppMetricaConfig.newConfigBuilder(BuildConfig.APPMETRICA_API_KEY).build())
            AppMetrica.enableActivityAutoTracking(this)
        }
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(SvgDecoder.Factory()) }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("images")).maxSizeBytes(64L * 1024 * 1024).build() }
        .crossfade(false).build()
}
