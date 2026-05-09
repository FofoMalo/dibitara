package com.dibitara.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.dibitara.app.presentation.AppViewModel
import com.dibitara.app.presentation.navigation.DibitaraNavGraph
import com.dibitara.app.presentation.common.theme.DibitaraTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activité unique de l'application (Single Activity Pattern).
 * AppCompatActivity (et non ComponentActivity) est requis car BiometricPrompt
 * a besoin d'un FragmentActivity pour afficher son dialogue.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Instancié ici pour que GenerateMonthlyRecurringUseCase s'exécute dès le démarrage
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DibitaraTheme {
                DibitaraNavGraph()
            }
        }
    }
}
