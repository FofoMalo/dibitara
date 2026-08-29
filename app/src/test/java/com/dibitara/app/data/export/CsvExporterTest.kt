package com.dibitara.app.data.export

import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Debt
import com.dibitara.app.domain.model.DebtType
import com.dibitara.app.domain.model.ExportData
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.model.SavingsType
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.model.SubCategory
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.model.VehicleEntryType
import com.dibitara.app.domain.model.VehicleRentalEntry
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CsvExporterTest {

    private fun donneesVides() = ExportData(
        enfants         = emptyList(),
        transactions    = emptyList(),
        budgets         = emptyList(),
        epargne         = emptyList(),
        immobilier      = emptyList(),
        scpi            = emptyList(),
        airbnb          = emptyList(),
        vehiculeLocatif = emptyList(),
        dettes          = emptyList(),
        actifsLibres    = emptyList(),
        epargneSalariale = emptyList(),
        sousCategoriesPerso  = emptyList(),
        comptesBancaires     = emptyList(),
        enveloppesBudget     = emptyList(),
        reglesCategorisation = emptyList(),
        versementsMensuels   = emptyList()
    )

    @Test
    fun `le CSV contient les dix sections`() {
        val csv = CsvExporter.generer(donneesVides())
        assertTrue(csv.contains("# TRANSACTIONS"))
        assertTrue(csv.contains("# BUDGETS"))
        assertTrue(csv.contains("# EPARGNE"))
        assertTrue(csv.contains("# IMMOBILIER"))
        assertTrue(csv.contains("# SCPI"))
        assertTrue(csv.contains("# AIRBNB"))
        assertTrue(csv.contains("# VEHICULE_LOCATIF"))
        assertTrue(csv.contains("# DETTES"))
        assertTrue(csv.contains("# ACTIFS_LIBRES"))
        assertTrue(csv.contains("# EPARGNE_SALARIALE"))
    }

    @Test
    fun `une entrée revenu et une charge du véhicule locatif sont correctement sérialisées`() {
        val revenu = VehicleRentalEntry(
            id = 1L, label = "Location weekend", entryType = VehicleEntryType.REVENU,
            amountCents = 15000L, date = LocalDate.of(2026, 5, 10), currency = Currency.EUR
        )
        val charge = VehicleRentalEntry(
            id = 2L, label = "Vidange", entryType = VehicleEntryType.CHARGE,
            amountCents = 8000L, date = LocalDate.of(2026, 5, 12), currency = Currency.EUR
        )
        val data = donneesVides().copy(vehiculeLocatif = listOf(revenu, charge))
        val csv = CsvExporter.generer(data)

        assertTrue(csv.contains("Location weekend;Revenu;15000"))
        assertTrue(csv.contains("Vidange;Charge;8000"))
    }

    @Test
    fun `une transaction est correctement sérialisée`() {
        val transaction = Transaction(
            id = 42L, amountCents = 1250L, currency = Currency.EUR,
            category = Category.ALIMENTATION, type = TransactionType.EXPENSE,
            date = LocalDate.of(2026, 5, 22), note = "Courses"
        )
        val data = donneesVides().copy(transactions = listOf(transaction))
        val csv = CsvExporter.generer(data)

        assertTrue(csv.contains("42"))
        assertTrue(csv.contains("2026-05-22"))
        assertTrue(csv.contains("1250"))
        assertTrue(csv.contains("EUR"))
        assertTrue(csv.contains("Alimentation"))
        assertTrue(csv.contains("Courses"))
    }

    @Test
    fun `les valeurs avec point-virgule sont entre guillemets`() {
        val transaction = Transaction(
            id = 1L, amountCents = 100L, currency = Currency.EUR,
            category = Category.AUTRE, type = TransactionType.EXPENSE,
            date = LocalDate.now(), note = "Truc;bidule"
        )
        val data = donneesVides().copy(transactions = listOf(transaction))
        val csv = CsvExporter.generer(data)
        assertTrue(csv.contains("\"Truc;bidule\""))
    }

    @Test
    fun `les guillemets dans les valeurs sont doublés`() {
        val transaction = Transaction(
            id = 1L, amountCents = 100L, currency = Currency.EUR,
            category = Category.AUTRE, type = TransactionType.EXPENSE,
            date = LocalDate.now(), note = "Café \"Kawa\""
        )
        val data = donneesVides().copy(transactions = listOf(transaction))
        val csv = CsvExporter.generer(data)
        assertTrue(csv.contains("\"Café \"\"Kawa\"\"\""))
    }

    @Test
    fun `un budget SCPI avec parts fractionnaires est sérialisé`() {
        val scpi = ScpiInvestment(
            id = 1L, label = "SCPI Test", sharesCount = 2.2,
            shareValueCents = 100000L, monthlyContributionCents = 5000L,
            currency = Currency.EUR, updatedAt = LocalDate.now()
        )
        val data = donneesVides().copy(scpi = listOf(scpi))
        val csv = CsvExporter.generer(data)
        assertTrue(csv.contains("2.2"))
    }
}
