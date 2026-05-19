package com.dibitara.app

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

/**
 * Point d'entrée de l'application.
 * L'annotation @HiltAndroidApp déclenche la génération du code Hilt
 * qui permet l'injection de dépendances dans toute l'application.
 */
@HiltAndroidApp
class DibitaraApp : Application() {

    override fun onCreate() {
        super.onCreate()
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
