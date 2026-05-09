package com.dibitara.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    /**
     * Demande POST_NOTIFICATIONS à l'exécution (obligatoire Android 13+).
     * Si l'utilisateur accorde la permission, on déclenche immédiatement
     * la vérification des notifications sans attendre le prochain lancement.
     */
    private val demandePermissionNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { accordee ->
        if (accordee) {
            appViewModel.relancerNotifications()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        demanderPermissionNotificationsSiNecessaire()
        setContent {
            DibitaraTheme {
                DibitaraNavGraph()
            }
        }
    }

    private fun demanderPermissionNotificationsSiNecessaire() {
        // POST_NOTIFICATIONS n'existe qu'à partir d'Android 13 (API 33)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val permissionAccordee = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!permissionAccordee) {
            demandePermissionNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
