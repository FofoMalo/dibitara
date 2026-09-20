package com.dibitara.app.presentation.common.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dibitara.app.R

/**
 * Polices variables (axe "wght") de la hiérarchie typographique du §6 de
 * CADRAGE_INDEPENDANCE_FINANCIERE.md : Outfit pour tout ce qui se lit (titres, phrases),
 * JetBrains Mono pour tout ce qui se compte (labels de section, montants). [FontVariation.Settings]
 * force l'instance de graisse demandée sur le fichier variable unique - sans lui, Android
 * affiche l'instance par défaut du fichier (Regular) quelle que soit la graisse déclarée
 * dans le [FontFamily], un bug invisible tant qu'on ne compare pas à l'écran.
 */
@OptIn(ExperimentalTextApi::class)
private fun instanceVariable(resId: Int, weight: FontWeight) = Font(
    resId             = resId,
    weight            = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val OutfitFamily = FontFamily(
    instanceVariable(R.font.outfit, FontWeight.Normal),
    instanceVariable(R.font.outfit, FontWeight.Medium),
    instanceVariable(R.font.outfit, FontWeight.SemiBold),
    instanceVariable(R.font.outfit, FontWeight.Bold)
)

val JetBrainsMonoFamily = FontFamily(
    instanceVariable(R.font.jetbrains_mono, FontWeight.Medium),
    instanceVariable(R.font.jetbrains_mono, FontWeight.SemiBold),
    instanceVariable(R.font.jetbrains_mono, FontWeight.Bold)
)

/**
 * Échelle à 4 niveaux (H1 / H2 / Montant / Explicatif) définie en CADRAGE_INDEPENDANCE_FINANCIERE.md
 * §6 : formalise ce que l'app fait déjà presque partout, plutôt qu'un nouveau vocabulaire.
 * H1 (titre d'écran) et Explicatif restent en Outfit sur les tailles Material3 existantes
 * (bodyMedium/titleLarge...) - seuls H2 et les montants ont besoin d'un style dédié, puisque
 * ce sont eux qui changent de police (mono) par rapport au comportement par défaut.
 */
object DibitaraType {
    /** Titre de section (H2) : mono, MAJUSCULES (à appliquer par l'appelant), petit, tracking large, 50% d'opacité. */
    val h2: TextStyle
        @Composable @ReadOnlyComposable get() = TextStyle(
            fontFamily    = JetBrainsMonoFamily,
            fontWeight    = FontWeight.SemiBold,
            fontSize      = 10.sp,
            letterSpacing = 1.5.sp,
            color         = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

    /** Montant héros : le chiffre principal d'une carte de synthèse (24-28sp). */
    val montantHero: TextStyle
        @Composable @ReadOnlyComposable get() = TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize   = 28.sp
        )

    /** Montant secondaire : un chiffre notable mais qui partage la vedette avec un autre (13-20sp). */
    val montantSecondaire: TextStyle
        @Composable @ReadOnlyComposable get() = TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 18.sp
        )

    /** Montant inline : une valeur chiffrée isolée dans une ligne compacte (11-13sp). */
    val montantInline: TextStyle
        @Composable @ReadOnlyComposable get() = TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Medium,
            fontSize   = 13.sp
        )
}
