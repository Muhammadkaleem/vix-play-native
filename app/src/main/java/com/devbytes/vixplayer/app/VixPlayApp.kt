package com.devbytes.vixplayer.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.devbytes.vixplayer.app.notifications.VixMessagingService
import com.tencent.mmkv.MMKV
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VixPlayApp : Application(), ImageLoaderFactory, Configuration.Provider {

    // Lets @HiltWorker-annotated workers (Library Pulse) receive constructor-injected
    // repositories. Requires removing the default WorkManagerInitializer in the manifest.
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        createPushChannel()
        initFirebase()
    }

    /** The FCM notification channel. Created unconditionally (not tied to Firebase init) so
     *  push notifications always have a channel to post to on Android 8+. */
    private fun createPushChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(VixMessagingService.CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    VixMessagingService.CHANNEL_ID,
                    "Announcements",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "News, tips and updates from VixPlay" }
            )
        }
    }

    /**
     * Enables the free Firebase products. Collection for the data-reporting ones is **on in
     * release, off in debug** so development builds never pollute the production dashboards
     * (mirrors the existing Crashlytics / AdUnits debug-vs-release split). Wrapped so a device
     * without Google Play Services can't take down startup.
     */
    private fun initFirebase() {
        runCatching {
            val collect = !BuildConfig.DEBUG
            // Crashlytics (crash + ANR + native). Independent of UMP ad-consent (diagnostic).
            FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = collect
            // Analytics — enables Firebase Analytics + powers In-App Messaging.
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(collect)
            // Performance Monitoring (app-start + custom traces; auto network instrumentation
            // needs the perf Gradle plugin, which isn't Gradle-9 compatible yet — SDK-only here).
            FirebasePerformance.getInstance().isPerformanceCollectionEnabled = collect

            // Remote Config — ready to fetch server-side params; harmless no-op until any are set.
            FirebaseRemoteConfig.getInstance().apply {
                setConfigSettingsAsync(
                    FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
                        .build()
                )
                fetchAndActivate()
            }
        }
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(VideoFrameDecoder.Factory()) }
        .crossfade(true)
        .build()
}
