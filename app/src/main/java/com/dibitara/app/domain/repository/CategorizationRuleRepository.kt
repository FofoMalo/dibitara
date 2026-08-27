package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.CategorizationRule

interface CategorizationRuleRepository {

    /** Insère ou remplace la règle pour ce libellé (clé unique = noteExact). */
    suspend fun upsert(rule: CategorizationRule)

    /**
     * Retourne la règle dont [noteExact] correspond au libellé donné
     * (comparaison insensible à la casse, espaces tronqués), ou null.
     */
    suspend fun getRuleForNote(note: String): CategorizationRule?
}
