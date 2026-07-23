package com.dibitara.app.data.export

import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.model.CustomAsset
import com.dibitara.app.domain.model.Debt
import com.dibitara.app.domain.model.EmployeeSavings
import com.dibitara.app.domain.model.ExportData
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.VehicleRentalEntry

/**
 * Génère un fichier CSV avec séparateur ";" (compatible Excel en locale française).
 * Chaque type de données est regroupé dans une section précédée d'un commentaire "# NOM".
 * Les montants sont en centimes pour éviter tout arrondi flottant.
 */
object CsvExporter {

    fun generer(data: ExportData): String = buildString {
        appendSection("TRANSACTIONS",    lignesTransactions(data.transactions))
        appendSection("BUDGETS",         lignesBudgets(data.budgets))
        appendSection("EPARGNE",         lignesEpargne(data.epargne))
        appendSection("IMMOBILIER",      lignesImmobilier(data.immobilier))
        appendSection("SCPI",            lignesScpi(data.scpi))
        appendSection("AIRBNB",          lignesAirbnb(data.airbnb))
        appendSection("VEHICULE_LOCATIF", lignesVehiculeLocatif(data.vehiculeLocatif))
        appendSection("DETTES",          lignesDettes(data.dettes))
        appendSection("ACTIFS_LIBRES",   lignesActifsLibres(data.actifsLibres))
        appendSection("EPARGNE_SALARIALE", lignesEpargneSalariale(data.epargneSalariale))
    }

    // ─── Sections ─────────────────────────────────────────────────────────────

    private fun StringBuilder.appendSection(titre: String, lignes: List<String>) {
        append("# $titre\n")
        lignes.forEach { append(it).append('\n') }
        append('\n')
    }

    // ─── Convertisseurs par type ───────────────────────────────────────────────

    private fun lignesTransactions(list: List<Transaction>): List<String> {
        val entete = "id;date;type;montant_centimes;devise;categorie;sous_categorie;note;" +
            "recurrent;frequence;id_modele_recurrent"
        return listOf(entete) + list.map { t ->
            "${t.id};${t.date};${t.type.name};${t.amountCents};${t.currency.isoCode};" +
            "${t.category.displayName};${t.subCategory?.displayName ?: ""};" +
            "${echapper(t.note)};${t.isRecurring};" +
            "${t.recurrenceFrequency?.name ?: ""};${t.sourceRecurringId ?: ""}"
        }
    }

    private fun lignesBudgets(list: List<Budget>): List<String> {
        val entete = "id;mois;annee;alloue_centimes;depense_centimes;devise"
        return listOf(entete) + list.map { b ->
            "${b.id};${b.month};${b.year};${b.allocatedCents};${b.spentCents};${b.currency.isoCode}"
        }
    }

    private fun lignesEpargne(list: List<SavingsAccount>): List<String> {
        val entete = "id;type;libelle;solde_centimes;versement_mensuel_centimes;devise;id_enfant;mise_a_jour"
        return listOf(entete) + list.map { s ->
            "${s.id};${s.type.displayName};${echapper(s.label)};${s.currentBalanceCents};" +
            "${s.monthlyContributionCents};${s.currency.isoCode};${s.childId ?: ""};${s.updatedAt}"
        }
    }

    private fun lignesImmobilier(list: List<RealEstateAsset>): List<String> {
        val entete = "id;libelle;valeur_actuelle_centimes;devise;mise_a_jour"
        return listOf(entete) + list.map { r ->
            "${r.id};${echapper(r.label)};${r.currentValueCents};${r.currency.isoCode};${r.updatedAt}"
        }
    }

    private fun lignesScpi(list: List<ScpiInvestment>): List<String> {
        val entete = "id;libelle;nb_parts;valeur_part_centimes;versement_mensuel_centimes;devise;mise_a_jour"
        return listOf(entete) + list.map { s ->
            "${s.id};${echapper(s.label)};${s.sharesCount};${s.shareValueCents};" +
            "${s.monthlyContributionCents};${s.currency.isoCode};${s.updatedAt}"
        }
    }

    private fun lignesAirbnb(list: List<AirbnbRental>): List<String> {
        val entete = "id;bien;montant_centimes;date;devise"
        return listOf(entete) + list.map { a ->
            "${a.id};${echapper(a.propertyLabel)};${a.amountCents};${a.date};${a.currency.isoCode}"
        }
    }

    private fun lignesVehiculeLocatif(list: List<VehicleRentalEntry>): List<String> {
        val entete = "id;libelle;type;montant_centimes;date;devise"
        return listOf(entete) + list.map { v ->
            "${v.id};${echapper(v.label)};${v.entryType.displayName};${v.amountCents};${v.date};${v.currency.isoCode}"
        }
    }

    private fun lignesDettes(list: List<Debt>): List<String> {
        val entete = "id;libelle;type;total_centimes;mensualite_centimes;devise;mise_a_jour"
        return listOf(entete) + list.map { d ->
            "${d.id};${echapper(d.label)};${d.type.displayName};${d.totalCents};" +
            "${d.monthlyPaymentCents};${d.currency.isoCode};${d.updatedAt}"
        }
    }

    private fun lignesActifsLibres(list: List<CustomAsset>): List<String> {
        val entete = "id;libelle;valeur_totale_centimes;devise;mise_a_jour"
        return listOf(entete) + list.map { a ->
            "${a.id};${echapper(a.label)};${a.totalValueCents};${a.currency.isoCode};${a.updatedAt}"
        }
    }

    private fun lignesEpargneSalariale(list: List<EmployeeSavings>): List<String> {
        val entete = "id;type;libelle;solde_centimes;abondement_employeur_centimes;devise;mise_a_jour"
        return listOf(entete) + list.map { e ->
            "${e.id};${e.type.displayName};${echapper(e.label)};${e.currentBalanceCents};" +
            "${e.employerContributionCents};${e.currency.isoCode};${e.updatedAt}"
        }
    }

    // ─── Utilitaire ───────────────────────────────────────────────────────────

    /**
     * Protège une valeur qui contient ";" ou des guillemets.
     * RFC 4180 : les guillemets sont doublés, la valeur est encadrée de guillemets.
     */
    private fun echapper(valeur: String): String {
        if (!valeur.contains(';') && !valeur.contains('"') && !valeur.contains('\n')) {
            return valeur
        }
        return "\"${valeur.replace("\"", "\"\"")}\""
    }
}
