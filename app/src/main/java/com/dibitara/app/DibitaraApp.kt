package com.dibitara.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Point d'entrée de l'application.
 * Implémente [Configuration.Provider] pour que WorkManager utilise [HiltWorkerFactory]
 * et puisse injecter des dépendances dans les Workers (@HiltWorker).
 */
@HiltAndroidApp
class DibitaraApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Requis par pdfbox-android avant toute extraction PDF
        PDFBoxResourceLoader.init(applicationContext)
        configurerCrashlytics()
    }

    /**
     * Active Crashlytics uniquement en production (release) et uniquement si
     * google-services.json est configuré (BuildConfig.CRASHLYTICS_ENABLED).
     * En mode debug ou sans Firebase configuré, aucun rapport n'est envoyé.
     */
    private fun configurerCrashlytics() {
        if (BuildConfig.CRASHLYTICS_ENABLED) {
            FirebaseCrashlytics.getInstance()
                .setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        }
    }
}
