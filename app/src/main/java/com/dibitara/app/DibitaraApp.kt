package com.dibitara.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Point d'entrée de l'application.
 * L'annotation @HiltAndroidApp déclenche la génération du code Hilt
 * qui permet l'injection de dépendances dans toute l'application.
 */
@HiltAndroidApp
class DibitaraApp : Application()
