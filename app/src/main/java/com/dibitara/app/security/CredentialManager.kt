package com.dibitara.app.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestionnaire centralisé des identifiants d'authentification locale.
 *
 * Stocke les hash (PBKDF2WithHmacSHA256) dans EncryptedSharedPreferences —
 * protégé par AES256-GCM via le KeyStore Android.
 * Le salt aléatoire (32 octets) est régénéré à chaque changement de secret,
 * ce qui empêche les attaques par dictionnaire pré-calculées (rainbow tables).
 *
 * Les fonctions de hash sont suspend et s'exécutent sur Dispatchers.IO
 * pour ne jamais bloquer le thread principal (PBKDF2 prend ~200–400 ms).
 */
@Singleton
class CredentialManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_FILE = "dibitara_auth_prefs"

        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PWD_HASH = "pwd_hash"
        private const val KEY_PWD_SALT = "pwd_salt"
        private const val KEY_EMAIL    = "email"

        // Paramètres PBKDF2 conformes aux recommandations OWASP 2024
        private const val ITERATIONS_PIN = 100_000
        private const val ITERATIONS_PWD = 310_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_BYTES = 32
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    }

    // Initialisation paresseuse — l'accès au KeyStore peut prendre quelques ms
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ─── PIN ──────────────────────────────────────────────────────────────────

    /** Retourne true si un PIN a déjà été enregistré. */
    fun isPinSetup(): Boolean = prefs.contains(KEY_PIN_HASH)

    /**
     * Enregistre un nouveau PIN (remplace l'ancien si existant).
     * Bloquant — doit être appelé via une coroutine (Dispatchers.IO géré en interne).
     */
    suspend fun setupPin(pin: String) = withContext(Dispatchers.IO) {
        val salt = generateSalt()
        val hash = hashWithPbkdf2(pin, salt, ITERATIONS_PIN)
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt.toHex())
            .apply()
    }

    /**
     * Vérifie un PIN contre le hash stocké.
     * Bloquant — Dispatchers.IO géré en interne.
     * @return true si le PIN est correct
     */
    suspend fun verifyPin(pin: String): Boolean = withContext(Dispatchers.IO) {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return@withContext false
        val storedSalt = prefs.getString(KEY_PIN_SALT, null)?.fromHex() ?: return@withContext false
        val inputHash  = hashWithPbkdf2(pin, storedSalt, ITERATIONS_PIN)
        storedHash == inputHash
    }

    // ─── Email + mot de passe ─────────────────────────────────────────────────

    /** Retourne true si un mot de passe local a été enregistré. */
    fun isPasswordSetup(): Boolean = prefs.contains(KEY_PWD_HASH)

    /** Retourne l'email enregistré, ou null si aucun mot de passe configuré. */
    fun getStoredEmail(): String? = prefs.getString(KEY_EMAIL, null)

    /**
     * Enregistre un email + mot de passe (remplace les anciens si existants).
     * Dispatchers.IO géré en interne.
     */
    suspend fun setupPassword(email: String, password: String) = withContext(Dispatchers.IO) {
        val salt = generateSalt()
        val hash = hashWithPbkdf2(password, salt, ITERATIONS_PWD)
        prefs.edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_PWD_HASH, hash)
            .putString(KEY_PWD_SALT, salt.toHex())
            .apply()
    }

    /**
     * Vérifie un email + mot de passe contre les données stockées.
     * Dispatchers.IO géré en interne.
     * @return true si les deux correspondent
     */
    suspend fun verifyPassword(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val storedEmail = prefs.getString(KEY_EMAIL, null) ?: return@withContext false
        if (!storedEmail.equals(email, ignoreCase = true)) return@withContext false
        val storedHash = prefs.getString(KEY_PWD_HASH, null) ?: return@withContext false
        val storedSalt = prefs.getString(KEY_PWD_SALT, null)?.fromHex() ?: return@withContext false
        val inputHash  = hashWithPbkdf2(password, storedSalt, ITERATIONS_PWD)
        storedHash == inputHash
    }

    // ─── Réinitialisation ────────────────────────────────────────────────────

    /**
     * Efface tous les secrets stockés (PIN + mot de passe + email).
     * La base de données Room n'est PAS touchée — les données financières sont conservées.
     * À n'appeler qu'après une vérification biométrique réussie.
     */
    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .remove(KEY_PWD_HASH)
            .remove(KEY_PWD_SALT)
            .remove(KEY_EMAIL)
            .apply()
    }

    // ─── Utilitaires cryptographiques ─────────────────────────────────────────

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun hashWithPbkdf2(input: String, salt: ByteArray, iterations: Int): String {
        val spec    = PBEKeySpec(input.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded.toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray {
        check(length % 2 == 0) { "Hex string doit avoir un nombre pair de caractères" }
        return ByteArray(length / 2) { i ->
            substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
