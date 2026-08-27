package com.dibitara.app.data.importcsv

import java.security.MessageDigest
import java.time.LocalDate

/**
 * Génère l'`externalId` de déduplication d'une transaction importée depuis un CSV.
 *
 * Un CSV brut ne fournit aucun identifiant stable (contrairement à l'UUID d'un
 * export TradeRepublic) : on calcule donc une empreinte SHA-256 de
 * `date | montant | libellé normalisé`, tronquée à 32 caractères hex.
 *
 * Changement volontaire par rapport à l'ancien `BredCategoriseur.genererExternalId`,
 * qui utilisait `String.hashCode()` (32 bits) : un historique CSV pluriannuel
 * représente trop de lignes pour un hash 32 bits (risque de collision non
 * négligeable). Le coût d'un vrai digest est ici sans importance.
 *
 * Limite assumée (voir CADRAGE_SPRINT_44_IMPORT_CSV.md §3) : deux transactions
 * réellement identiques le même jour (même montant, même libellé) produisent le
 * même id et n'en forment qu'une. Filet de sécurité : l'écran « Nettoyer les
 * doublons ».
 */
object ExternalIdGenerator {

    fun pour(date: LocalDate, amountCents: Long, note: String): String {
        val cle = "$date|$amountCents|${TexteNormalisation.cle(note)}"
        val empreinte = MessageDigest.getInstance("SHA-256")
            .digest(cle.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "csv_" + empreinte.take(32)
    }
}
