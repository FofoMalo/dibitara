package com.dibitara.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dibitara.app.presentation.navigation.DibitaraNavGraph
import com.dibitara.app.presentation.common.theme.DibitaraTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activité unique de l'application (Single Activity Pattern).
 * Elle héberge le graphe de navigation Compose — aucune logique métier ici.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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
