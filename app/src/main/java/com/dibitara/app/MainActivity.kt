package com.dibitara.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.dibitara.app.presentation.AppViewModel
import com.dibitara.app.presentation.navigation.DibitaraNavGraph
import com.dibitara.app.presentation.common.LocalMontantsMasques
import com.dibitara.app.presentation.common.theme.DibitaraTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activité unique de l'application (Single Activity Pattern).
 * AppCompatActivity (et non ComponentActivity) est requis car BiometricPrompt
 * a besoin d'un FragmentActivity pour afficher son dialogue.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Instancié ici pour que GenerateRecurringUseCase s'exécute dès le démarrage
    private val appViewModel: AppViewModel by viewModels()

    // Conservé pour transmettre les deep links des notifications quand l'activité
    // est déjà au premier plan (launchMode singleTop -> onNewIntent, pas onCreate)
    private lateinit var navController: NavHostController

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
        // `by viewModels()` est paresseux : sans cette lecture explicite, appViewModel n'est
        // jamais instancié (donc son init{} jamais exécuté) tant que la permission notifications
        // est déjà accordée - seul autre endroit où la propriété était lue (voir le callback ci-dessus).
        appViewModel
        demanderPermissionNotificationsSiNecessaire()
        setContent {
            DibitaraTheme {
                navController = rememberNavController()
                val masquerMontants by appViewModel.masquerMontants.collectAsState()
                CompositionLocalProvider(LocalMontantsMasques provides masquerMontants) {
                    DibitaraNavGraph(navController = navController)
                }
            }
        }
    }

    /**
     * En launchMode singleTop, un clic sur une notification alors que l'app est déjà
     * au premier plan ne redéclenche pas onCreate : Android appelle onNewIntent, qui ne
     * fait rien par défaut. Il faut transmettre l'intent au NavController à la main.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navController.handleDeepLink(intent)
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
