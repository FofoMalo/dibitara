package com.dibitara.app.data.local.entity

/**
 * Lecture défensive d'un enum depuis la base de données.
 * Si la valeur stockée est inconnue (migration incomplète, future valeur), on retourne [default]
 * plutôt que de crasher avec IllegalArgumentException.
 */
inline fun <reified T : Enum<T>> safeValueOf(value: String, default: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(default)
