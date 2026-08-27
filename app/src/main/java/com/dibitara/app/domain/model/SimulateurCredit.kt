package com.dibitara.app.domain.model

import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Calculs d'amortissement pour un crédit immobilier à taux fixe.
 * Toutes les fonctions sont pures - aucun effet de bord, testables unitairement.
 *
 * Formules utilisées (amortissement à taux fixe, mensualité constante) :
 *   - Mensualités restantes = ln(M / (M - C × r)) / ln(1 + r)
 *     où M = mensualité, C = capital restant, r = taux mensuel (= taux annuel / 12)
 *   - Intérêts totaux restants = M × N - C
 *     où N = mensualités restantes calculées ci-dessus
 */
object SimulateurCredit {

    data class ResultatSimulation(
        val moisActuels: Int,           // durée restante sans remboursement anticipé
        val moisApres: Int,             // durée restante après remboursement anticipé
        val moisEconomises: Int,        // = moisActuels - moisApres
        val interetsTotauxActuelsCents: Long,   // intérêts restants à payer sans RA
        val interetsTotauxApresCents: Long,     // intérêts restants à payer avec RA
        val interetsEconomisesCents: Long,      // = interetsTotauxActuels - interetsTotauxApres
        val nouvelleEcheanceAnneeMois: Pair<Int, Int>?  // (année, mois) de la fin du crédit après RA
    )

    /**
     * Simule un remboursement anticipé partiel.
     *
     * [capitalRestantCents]        : capital restant dû aujourd'hui
     * [tauxAnnuelPct]              : taux annuel en % (ex : 1.85)
     * [mensualiteCents]            : mensualité fixe (hors assurance)
     * [remboursementAnticipeCents] : montant du remboursement anticipé envisagé
     * [anneeDebutRemboursement]    : année à partir de laquelle on compte l'échéance
     * [moisDebutRemboursement]     : mois à partir duquel on compte l'échéance
     */
    fun simuler(
        capitalRestantCents: Long,
        tauxAnnuelPct: Double,
        mensualiteCents: Long,
        remboursementAnticipeCents: Long,
        anneeDebutRemboursement: Int = java.time.LocalDate.now().year,
        moisDebutRemboursement: Int  = java.time.LocalDate.now().monthValue
    ): ResultatSimulation? {
        if (tauxAnnuelPct <= 0 || mensualiteCents <= 0 || capitalRestantCents <= 0) return null

        val tauxMensuel = tauxAnnuelPct / 100.0 / 12.0
        val capital     = capitalRestantCents / 100.0
        val mensualite  = mensualiteCents / 100.0
        val ra          = remboursementAnticipeCents / 100.0

        // Vérification : la mensualité doit couvrir au moins les intérêts du premier mois
        if (mensualite <= capital * tauxMensuel) return null

        val moisActuels       = calculerMois(capital, tauxMensuel, mensualite)
        val interetsActuels   = (mensualite * moisActuels - capital).coerceAtLeast(0.0)

        val capitalApresRA    = (capital - ra).coerceAtLeast(0.0)
        val moisApres         = if (capitalApresRA <= 0) 0
                                else calculerMois(capitalApresRA, tauxMensuel, mensualite)
        val interetsApres     = if (capitalApresRA <= 0) 0.0
                                else (mensualite * moisApres - capitalApresRA).coerceAtLeast(0.0)

        val moisEconomises    = moisActuels - moisApres
        val interetsEconomises = interetsActuels - interetsApres

        // Date d'échéance après RA
        val echeance = if (moisApres > 0) {
            var annee = anneeDebutRemboursement
            var mois  = moisDebutRemboursement + moisApres
            annee += (mois - 1) / 12
            mois   = ((mois - 1) % 12) + 1
            Pair(annee, mois)
        } else null

        return ResultatSimulation(
            moisActuels                   = moisActuels,
            moisApres                     = moisApres,
            moisEconomises                = moisEconomises,
            interetsTotauxActuelsCents    = (interetsActuels   * 100).roundToInt().toLong(),
            interetsTotauxApresCents      = (interetsApres     * 100).roundToInt().toLong(),
            interetsEconomisesCents       = (interetsEconomises * 100).roundToInt().toLong(),
            nouvelleEcheanceAnneeMois     = echeance
        )
    }

    /** Nombre de mensualités restantes (formule amortissement taux fixe, arrondi au supérieur). */
    private fun calculerMois(capital: Double, tauxMensuel: Double, mensualite: Double): Int {
        if (tauxMensuel == 0.0) return ceil(capital / mensualite).toInt()
        return ceil(
            ln(mensualite / (mensualite - capital * tauxMensuel)) / ln(1 + tauxMensuel)
        ).toInt().coerceAtLeast(0)
    }

    /** Mensualité théorique d'un crédit (utile pour vérifier la cohérence des données). */
    fun calculerMensualite(capitalCents: Long, tauxAnnuelPct: Double, dureeMois: Int): Long {
        if (dureeMois <= 0 || tauxAnnuelPct < 0) return 0L
        val c = capitalCents / 100.0
        val r = tauxAnnuelPct / 100.0 / 12.0
        val m = if (r == 0.0) c / dureeMois
                else c * r * (1 + r).pow(dureeMois) / ((1 + r).pow(dureeMois) - 1)
        return (m * 100).roundToInt().toLong()
    }
}
