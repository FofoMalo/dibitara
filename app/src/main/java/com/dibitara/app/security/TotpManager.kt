package com.dibitara.app.security

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestion du TOTP (Time-based One-Time Password) conforme RFC 6238.
 *
 * Algorithme :
 *  1. Générer un secret aléatoire 20 octets encodé en Base32 (RFC 4648)
 *  2. HOTP(secret, counter) = tronquer HMAC-SHA1(secret, counter_8_octets)
 *  3. TOTP(secret) = HOTP(secret, floor(temps_unix / 30))
 *
 * Compatibilité : Google Authenticator, Authy, 1Password, Bitwarden…
 */
@Singleton
class TotpManager @Inject constructor() {

    companion object {
        private const val BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        private const val DIGITS       = 6
        private const val PERIOD       = 30L   // secondes par fenêtre TOTP
        private const val SECRET_BYTES = 20    // 160 bits = recommandation RFC 4226
    }

    // ─── Génération du secret ────────────────────────────────────────────────

    /** Génère un secret aléatoire 160 bits, renvoyé en Base32 pour Google Authenticator. */
    fun generateSecret(): String {
        val bytes = ByteArray(SECRET_BYTES)
        SecureRandom().nextBytes(bytes)
        return base32Encode(bytes)
    }

    // ─── URI otpauth:// ──────────────────────────────────────────────────────

    /**
     * Construit l'URI otpauth:// à encoder dans le QR code.
     * Format : otpauth://totp/Dibitara:email?secret=...&issuer=Dibitara&...
     */
    fun buildOtpAuthUri(secret: String, email: String): String =
        "otpauth://totp/Dibitara:${email}?secret=${secret}" +
        "&issuer=Dibitara&algorithm=SHA1&digits=${DIGITS}&period=${PERIOD}"

    // ─── Vérification ────────────────────────────────────────────────────────

    /**
     * Vérifie un code à 6 chiffres saisi par l'utilisateur.
     *
     * On accepte une fenêtre de ±1 pas (±30 s) pour compenser les décalages d'horloge
     * entre l'appareil et l'application d'authentification.
     *
     * @return true si le code correspond au pas courant, précédent ou suivant
     */
    fun verify(secret: String, code: String): Boolean {
        if (code.length != DIGITS || code.any { !it.isDigit() }) return false
        val step = System.currentTimeMillis() / 1000 / PERIOD
        return (-1..1).any { delta -> computeTotp(secret, step + delta) == code }
    }

    // ─── Calcul TOTP ─────────────────────────────────────────────────────────

    private fun computeTotp(secret: String, timeStep: Long): String {
        val key     = base32Decode(secret)
        val mac     = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        // Le compteur HOTP est un entier 64 bits big-endian
        val counter = ByteBuffer.allocate(8).putLong(timeStep).array()
        val hash    = mac.doFinal(counter)
        // Troncature dynamique (RFC 4226 §5.3)
        val offset  = hash[19].toInt() and 0x0f
        val binary  = ((hash[offset].toInt()     and 0x7f) shl 24) or
                      ((hash[offset + 1].toInt() and 0xff) shl 16) or
                      ((hash[offset + 2].toInt() and 0xff) shl 8)  or
                       (hash[offset + 3].toInt() and 0xff)
        return "%0${DIGITS}d".format(binary % 1_000_000)
    }

    // ─── Base32 RFC 4648 ─────────────────────────────────────────────────────

    private fun base32Encode(bytes: ByteArray): String {
        val sb = StringBuilder()
        var buffer   = 0
        var bitsLeft = 0
        for (b in bytes) {
            buffer    = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                bitsLeft -= 5
                sb.append(BASE32_CHARS[(buffer shr bitsLeft) and 0x1F])
            }
        }
        if (bitsLeft > 0) sb.append(BASE32_CHARS[(buffer shl (5 - bitsLeft)) and 0x1F])
        return sb.toString()
    }

    private fun base32Decode(encoded: String): ByteArray {
        val clean  = encoded.uppercase().replace("=", "")
        val result = mutableListOf<Byte>()
        var buffer   = 0
        var bitsLeft = 0
        for (c in clean) {
            val idx = BASE32_CHARS.indexOf(c)
            if (idx < 0) continue   // ignorer les caractères non reconnus
            buffer    = (buffer shl 5) or idx
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                result.add((buffer shr bitsLeft and 0xFF).toByte())
            }
        }
        return result.toByteArray()
    }
}
