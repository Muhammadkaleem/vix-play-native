package com.devbytes.vixplayer.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
        // Crash reporting on in release, off in debug — dev crashes never pollute the real
        // project (mirrors the AdUnits debug/release split). Diagnostic only; kept independent
        // of the UMP ad-consent flow.
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(VideoFrameDecoder.Factory()) }
        .crossfade(true)
        .build()
}
