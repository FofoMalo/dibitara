package com.dibitara.app.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import androidx.room.withTransaction
import com.dibitara.app.data.local.database.DibitaraDatabase
import com.dibitara.app.data.local.entity.AirbnbRentalEntity
import com.dibitara.app.data.local.entity.BankAccountEntity
import com.dibitara.app.data.local.entity.BudgetEntity
import com.dibitara.app.data.local.entity.CategorizationRuleEntity
import com.dibitara.app.data.local.entity.CategoryEnvelopeEntity
import com.dibitara.app.data.local.entity.ChildEntity
import com.dibitara.app.data.local.entity.CustomAssetEntity
import com.dibitara.app.data.local.entity.CustomSubCategoryEntity
import com.dibitara.app.data.local.entity.DebtEntity
import com.dibitara.app.data.local.entity.EmployeeSavingsEntity
import com.dibitara.app.data.local.entity.MonthlyVersementEntity
import com.dibitara.app.data.local.entity.RealEstateAssetEntity
import com.dibitara.app.data.local.entity.SavingsAccountEntity
import com.dibitara.app.data.local.entity.ScpiInvestmentEntity
import com.dibitara.app.data.local.entity.TransactionEntity
import com.dibitara.app.data.local.entity.VehicleRentalEntryEntity
import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.BankAccount
import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.model.CategorizationRule
import com.dibitara.app.domain.model.CategoryEnvelope
import com.dibitara.app.domain.model.Child
import com.dibitara.app.domain.model.CustomAsset
import com.dibitara.app.domain.model.CustomSubCategory
import com.dibitara.app.domain.model.Debt
import com.dibitara.app.domain.model.EmployeeSavings
import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.VehicleRentalEntry
import com.dibitara.app.domain.repository.RestoreRepository
import com.dibitara.app.domain.repository.RestoreResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Type
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implémentation de [RestoreRepository].
 *
 * Algorithme :
 *  1. Lit le JSON depuis l'URI via ContentResolver (Storage Access Framework - aucune
 *     permission WRITE_EXTERNAL_STORAGE requise, Drive apparaît naturellement dans le picker).
 *  2. Désérialise la racine en [JsonObject] pour extraire chaque liste par clé.
 *  3. Dans **une seule transaction Room** : vide toutes les tables puis réinsère chaque
 *     entité via son DAO, en conservant les identifiants d'origine (clés étrangères
 *     childId, debtId, bankAccountId, customSubCategoryId… restent cohérentes). Si une
 *     insertion échoue, la transaction est annulée et la base d'origine reste intacte.
 *
 * Couvre 16 des 18 tables. Volontairement exclues : `patrimoine_snapshots` et
 * `asset_valuation_snapshots` (historiques qui se reconstruisent d'eux-mêmes et
 * gonfleraient le fichier). Un fichier de sauvegarde antérieur à 2026-08 n'a pas les
 * clés `versements_mensuels`, `sous_categories_perso`, etc. : [parseList] renvoie alors
 * une liste vide, sans erreur.
 */
@Singleton
class RestoreRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: DibitaraDatabase
) : RestoreRepository {

    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, AdaptateurLocalDate())
        .create()

    override suspend fun restaurer(uri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        try {
            // ── Étape 1 : lire le JSON ────────────────────────────────────────
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return@withContext RestoreResult.Error("Impossible d'ouvrir le fichier sélectionné.")

            if (json.isBlank()) {
                return@withContext RestoreResult.Error("Le fichier est vide.")
            }

            // ── Étape 2 : désérialiser la racine ─────────────────────────────
            val jsonObj = gson.fromJson(json, JsonObject::class.java)

            val enfants      = parseList<Child>(jsonObj, "enfants")
            val transactions = parseList<Transaction>(jsonObj, "transactions")
            val budgets      = parseList<Budget>(jsonObj, "budgets")
            val epargne      = parseList<SavingsAccount>(jsonObj, "epargne")
            val immobilier   = parseList<RealEstateAsset>(jsonObj, "immobilier")
            val scpi         = parseList<ScpiInvestment>(jsonObj, "scpi")
            val airbnb       = parseList<AirbnbRental>(jsonObj, "airbnb")
            val vehiculeLocatif = parseList<VehicleRentalEntry>(jsonObj, "vehicule_locatif")
            val dettes       = parseList<Debt>(jsonObj, "dettes")
            val actifs       = parseList<CustomAsset>(jsonObj, "actifs_libres")
            val epargneSal   = parseList<EmployeeSavings>(jsonObj, "epargne_salariale")
            val sousCategories = parseList<CustomSubCategory>(jsonObj, "sous_categories_perso")
            val comptesBancaires = parseList<BankAccount>(jsonObj, "comptes_bancaires")
            val enveloppes   = parseList<CategoryEnvelope>(jsonObj, "enveloppes_budget")
            val regles       = parseList<CategorizationRule>(jsonObj, "regles_categorisation")
            val versements   = parseList<MonthlyVersement>(jsonObj, "versements_mensuels")

            // ── Étape 3 : tout dans une transaction (rollback si une insertion échoue) ──
            database.withTransaction {
                TABLES_A_VIDER.forEach { table ->
                    database.openHelper.writableDatabase.execSQL("DELETE FROM $table")
                }

                // Ordre : les "parents" avant ce qui les référence (childId, bankAccountId,
                // customSubCategoryId). Les FK ne sont pas déclarées en base mais l'ordre
                // garde les données cohérentes à la lecture.
                enfants.forEach          { database.childDao().insert(ChildEntity.fromDomain(it)) }
                comptesBancaires.forEach { database.bankAccountDao().upsert(BankAccountEntity.fromDomain(it)) }
                sousCategories.forEach   { database.customSubCategoryDao().upsert(CustomSubCategoryEntity.fromDomain(it)) }
                transactions.forEach     { database.transactionDao().insert(TransactionEntity.fromDomain(it)) }
                budgets.forEach          { database.budgetDao().upsert(BudgetEntity.fromDomain(it)) }
                epargne.forEach          { database.savingsAccountDao().insert(SavingsAccountEntity.fromDomain(it)) }
                immobilier.forEach       { database.realEstateAssetDao().insert(RealEstateAssetEntity.fromDomain(it)) }
                scpi.forEach             { database.scpiInvestmentDao().insert(ScpiInvestmentEntity.fromDomain(it)) }
                airbnb.forEach           { database.airbnbRentalDao().insert(AirbnbRentalEntity.fromDomain(it)) }
                vehiculeLocatif.forEach  { database.vehicleRentalEntryDao().insert(VehicleRentalEntryEntity.fromDomain(it)) }
                dettes.forEach           { database.debtDao().insert(DebtEntity.fromDomain(it)) }
                actifs.forEach           { database.customAssetDao().insert(CustomAssetEntity.fromDomain(it)) }
                epargneSal.forEach       { database.employeeSavingsDao().insert(EmployeeSavingsEntity.fromDomain(it)) }
                enveloppes.forEach       { database.categoryEnvelopeDao().upsert(CategoryEnvelopeEntity.fromDomain(it)) }
                regles.forEach           { database.categorizationRuleDao().upsert(CategorizationRuleEntity.fromDomain(it)) }
                versements.forEach       { database.monthlyVersementDao().insert(MonthlyVersementEntity.fromDomain(it)) }
            }

            val total = enfants.size + transactions.size + budgets.size + epargne.size +
                immobilier.size + scpi.size + airbnb.size + vehiculeLocatif.size + dettes.size +
                actifs.size + epargneSal.size + sousCategories.size + comptesBancaires.size +
                enveloppes.size + regles.size + versements.size

            RestoreResult.Success(total)

        } catch (e: Exception) {
            RestoreResult.Error(e.message ?: "Erreur inconnue lors de la restauration.")
        }
    }

    /**
     * Extrait et désérialise une liste depuis un [JsonObject] selon la clé [key].
     * Retourne une liste vide si la clé est absente ou si le tableau est vide.
     */
    private inline fun <reified T> parseList(jsonObj: JsonObject, key: String): List<T> {
        val element = jsonObj.get(key) ?: return emptyList()
        if (!element.isJsonArray) return emptyList()
        val type = object : TypeToken<List<T>>() {}.type
        return gson.fromJson(element, type) ?: emptyList()
    }

    /** Sérialise/désérialise LocalDate en chaîne ISO-8601 - identique à JsonExporter. */
    private class AdaptateurLocalDate : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        override fun serialize(src: LocalDate, typeOfSrc: Type, ctx: JsonSerializationContext) =
            JsonPrimitive(src.toString())

        override fun deserialize(json: JsonElement, typeOfT: Type, ctx: JsonDeserializationContext) =
            LocalDate.parse(json.asString)
    }

    private companion object {
        /**
         * Toutes les tables de [DibitaraDatabase], vidées avant réinsertion pour repartir
         * d'une base propre (même sémantique que l'ancien clearAllTables, mais dans la même
         * transaction que les inserts). `patrimoine_snapshots` et `asset_valuation_snapshots`
         * sont vidées aussi : le fichier ne les contient pas, elles se reconstruisent ensuite.
         */
        val TABLES_A_VIDER = listOf(
            "transactions", "budgets", "children", "debts", "savings_accounts",
            "real_estate_assets", "scpi_investments", "airbnb_rentals",
            "custom_sub_categories", "monthly_versements", "custom_assets",
            "employee_savings", "patrimoine_snapshots", "categorization_rules",
            "category_envelopes", "vehicle_rental_entries", "asset_valuation_snapshots",
            "bank_accounts"
        )
    }
}
