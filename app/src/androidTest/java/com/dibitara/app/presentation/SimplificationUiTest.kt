package com.dibitara.app.presentation

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.dibitara.app.MainActivity
import com.dibitara.app.domain.model.*
import com.dibitara.app.presentation.common.theme.DibitaraTheme
import com.dibitara.app.presentation.settings.SettingsScreen
import com.dibitara.app.presentation.expenses.ExpenseItem
import com.dibitara.app.presentation.budget.BilanBudgetCard
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** Régression petite largeur + police agrandie : vérifier la mise en page, pas seulement la présence du texte. */
class SimplificationUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun show(content: @Composable () -> Unit) {
        rule.activity.runOnUiThread {
            rule.activity.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                    DibitaraTheme { androidx.compose.material3.Surface(Modifier.width(320.dp).fillMaxHeight()) { Box { content() } } }
                }
            }
        }
        rule.waitForIdle()
    }
    private fun screenshot(name: String) {
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        java.io.File(rule.activity.cacheDir, name).outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    private fun noOverflow(text: String) {
        val results = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(text, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertTrue(results.isNotEmpty())
        val r = results.first()
        // Compose peut conserver un paragraphe plus large que le texte dessiné.
        // Vérifier chaque ligne visible évite ce faux positif sans accepter de troncature.
        assertFalse("Hauteur tronquée : $text", r.didOverflowHeight)
        for (line in 0 until r.lineCount) {
            assertFalse("Ligne élidée : $text", r.isLineEllipsized(line))
            assertTrue("Débordement gauche : $text", r.getLineLeft(line) >= -1f)
            assertTrue("Débordement droit : $text", r.getLineRight(line) <= r.size.width + 1f)
        }
        assertEquals(text.length, r.getLineEnd(r.lineCount - 1))
    }
    @Test fun deviseCompleteEtRetourAuxParametres() {
        show { SettingsScreen() }
        rule.onNodeWithText("Devises et taux de change").performScrollTo().performClick()
        rule.onNodeWithText("Dollar canadien").performScrollTo().assertIsDisplayed()
        screenshot("qa-devises.png")
        noOverflow("Dollar canadien")
        rule.onNodeWithText("Retour").performScrollTo().performClick()
        rule.onNodeWithText("Sauvegardes et restauration").assertExists()
    }
    @Test fun transactionLongueLisibleEtOuvertureDirecte() {
        var edited = false
        val label = "Un commerçant avec un libellé particulièrement long"
        show { ExpenseItem(Transaction(1, 123456789, Currency.CAD, Category.ALIMENTATION,
            TransactionType.EXPENSE, LocalDate.of(2026, 9, 1), note = label), null, onEdit = { edited = true }, onDelete = {}) }
        noOverflow(label)
        screenshot("qa-transaction.png")
        rule.onNodeWithText(label).performClick()
        assertTrue(edited)
    }
    @Test fun bilanAccessibleAvecPoliceAgrandie() {
        show { BilanBudgetCard(123456789, 23456789, 100000000, null, Currency.CAD, {}, {}) }
        rule.onNodeWithText("Bilan du mois").assertIsDisplayed()
        screenshot("qa-budget.png")
        noOverflow("Bilan du mois")
        noOverflow("Définir →")
        rule.onNodeWithText("Comprendre ce montant").performClick()
        rule.onNodeWithText("Bilan et budget du mois").assertIsDisplayed()
    }
}
