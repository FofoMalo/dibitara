package com.dibitara.app.domain.usecase

import com.dibitara.app.data.local.database.DibitaraDatabase
import com.dibitara.app.domain.repository.UserPreferencesRepository
import com.dibitara.app.security.CredentialManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Efface définitivement toutes les données personnelles de l'appareil (RGPD Art. 17).
 *
 * Ordre d'effacement intentionnel :
 *  1. Base de données Room (transactions, investissements, budgets, etc.)
 *  2. DataStore (préférences utilisateur)
 *  3. Credentials chiffrés (PIN, TOTP) - en dernier pour éviter un état incohérent
 *     si l'opération était interrompue avant la fin.
 *
 * Exception architecturale assumée : [DibitaraDatabase] est injecté directement
 * pour accéder à [RoomDatabase.clearAllTables], car exprimer cette opération
 * via 16 méthodes de repository serait une sur-ingénierie pour une action unique.
 *
 * Après l'appel, l'application doit naviguer vers l'écran de configuration du PIN
 * car les credentials n'existent plus.
 */
class SupprimerToutesDonneesUseCase @Inject constructor(
    private val database              : DibitaraDatabase,
    private val userPreferencesRepo   : UserPreferencesRepository,
    private val credentialManager     : CredentialManager
) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        // Étape 1 : vide toutes les tables Room sans toucher au schéma
        database.clearAllTables()

        // Étape 2 : efface toutes les préférences DataStore
        userPreferencesRepo.clearAll()

        // Étape 3 : efface PIN, mot de passe, email et TOTP secret
        credentialManager.clearCredentials()
    }
}
