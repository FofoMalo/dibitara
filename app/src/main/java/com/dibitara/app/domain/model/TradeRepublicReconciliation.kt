package com.dibitara.app.domain.model

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/** Clés conservatrices : pas de rapprochement approximatif de noms de marchands. */
object TradeRepublicReconciliation {
    fun normaliser(libelle: String): String = Normalizer.normalize(libelle, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ").trim()

    fun carte(marchand: String): String? = normaliser(marchand).takeIf { it.isNotEmpty() }?.let { "card:$it" }
    fun plan(support: String): String? = normaliser(support).takeIf { it.isNotEmpty() }?.let { "plan:$it" }
    fun virement(expediteur: String): String? = normaliser(expediteur).takeIf { it.isNotEmpty() }?.let { "virement:$it" }

    fun cleHistorique(tx: Transaction): String? = tx.reconciliationKey ?: when {
        tx.importSource != "trade_republic_notification" -> null
        tx.note == "Roundup investi" -> "roundup"
        tx.note.startsWith("Plan d’épargne · ") -> plan(tx.note.removePrefix("Plan d’épargne · "))
        tx.category != Category.INVESTISSEMENT -> carte(tx.note)
        else -> null
    }

    fun memeMouvement(a: Transaction, b: Transaction): Boolean =
        a.amountCents == b.amountCents && a.currency == b.currency && a.type == b.type &&
            abs(a.date.toEpochDay() - b.date.toEpochDay()) <= 1 &&
            (a.bankAccountId == null || b.bankAccountId == null || a.bankAccountId == b.bankAccountId)

    /** Une clé manquante est une incertitude à exposer, pas une preuve d’absence de doublon. */
    fun candidats(tx: Transaction, autres: List<Transaction>): List<Transaction> = autres.filter {
        val a = cleHistorique(tx)
        val b = cleHistorique(it)
        memeMouvement(tx, it) &&
            (a == null || b == null || a == b ||
                // Si un export décrit un Roundup comme une exécution de plan,
                // Sans support dans la notification Roundup, ne pas conclure à deux opérations.
                (a == "roundup" && b.startsWith("plan:")) ||
                (b == "roundup" && a.startsWith("plan:")))
    }

    fun verifierUnique(tx: Transaction, candidats: List<Transaction>): Transaction? {
        if (candidats.isEmpty()) return null
        require(candidats.size == 1 && cleHistorique(tx) != null && cleHistorique(tx) == cleHistorique(candidats.single())) {
            "Rapprochement TradeRepublic à vérifier : ${tx.date}, ${tx.amountCents / 100.0} ${tx.currency.symbol}, ${tx.note}. " +
                "Plusieurs correspondances ou informations insuffisantes. Aucune écriture de cet import n’a été conservée."
        }
        return candidats.single()
    }
}
