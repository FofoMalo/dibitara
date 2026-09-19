package com.dibitara.app.data.export

import com.dibitara.app.domain.model.*
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BackupJsonVerifierTest {
    private fun donnees() = ExportData(
        enfants = emptyList(), transactions = listOf(Transaction(amountCents = 4866, currency = Currency.EUR,
            category = Category.INVESTISSEMENT, type = TransactionType.EXPENSE, date = LocalDate.of(2026, 9, 9),
            externalId = "csv-1", notificationExternalId = "live-1", reconciliationKey = "roundup")),
        budgets = emptyList(), epargne = emptyList(), immobilier = emptyList(), scpi = emptyList(), airbnb = emptyList(),
        vehiculeLocatif = emptyList(), dettes = emptyList(), actifsLibres = emptyList(), epargneSalariale = emptyList(),
        sousCategoriesPerso = emptyList(), comptesBancaires = emptyList(), enveloppesBudget = emptyList(),
        reglesCategorisation = emptyList(), versementsMensuels = emptyList(), objectifsEpargne = emptyList())

    @Test fun `export réel valide conserve les identifiants de réconciliation`() {
        val json = JsonExporter.generer(donnees(), "4.10.0")
        assertEquals(1, BackupJsonVerifier.verifier(json))
        val tx = JsonParser.parseString(json).asJsonObject.getAsJsonArray("transactions")[0].asJsonObject
        assertEquals("csv-1", tx["externalId"].asString)
        assertEquals("live-1", tx["notificationExternalId"].asString)
        assertEquals("roundup", tx["reconciliationKey"].asString)
        assertEquals(4866, tx["amountCents"].asInt)
    }

    @Test fun `export incomplet tronqué et tableau remplacé sont refusés`() {
        val valide = JsonExporter.generer(donnees(), "4.10.0")
        val incomplet = JsonParser.parseString(valide).asJsonObject.apply { remove("enfants") }
        val mauvaiseForme = JsonParser.parseString(valide).asJsonObject.apply { addProperty("transactions", "invalide") }
        listOf("", "{}", valide.take(valide.length / 2), incomplet.toString(), mauvaiseForme.toString()).forEach {
            assertThrows(Exception::class.java) { BackupJsonVerifier.verifier(it) }
        }
    }

    @Test fun `une base vide mais complète peut être sauvegardée sans supprimer les anciennes copies`() {
        assertEquals(0, BackupJsonVerifier.verifier(JsonExporter.generer(donnees().copy(transactions = emptyList()), "4.10.0")))
    }
}
