package com.dibitara.app.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.dibitara.app.domain.model.BankProvider
import com.dibitara.app.domain.model.Category

/**
 * Couleur et icône STABLES par catégorie de dépense (refonte UX/UI 2026-08).
 *
 * Remplace l'ancienne indexation positionnelle (DONUT_COULEURS[i % size] dans
 * DonutChart.kt) qui faisait changer la couleur d'une catégorie d'un mois à
 * l'autre selon son rang dans le tri par montant décroissant.
 *
 * Réutilisé par : le donut Budget, le graphique "Valeur par actif" des
 * Placements et les icônes de ligne de l'écran Transactions.
 */
fun Category.chartColor(): Color = when (this) {
    Category.ALIMENTATION          -> Color(0xFFA8C7A0) // sage
    Category.LOGEMENT              -> Color(0xFFF5C542) // or
    Category.TRANSPORT             -> Color(0xFF7FB6B0) // teal
    Category.SANTE                 -> Color(0xFFE5687A) // rouge éclairci
    Category.LOISIRS               -> Color(0xFFD4B896) // ton chaud
    Category.ABONNEMENTS           -> Color(0xFFC79AB0) // mauve
    Category.INVESTISSEMENT        -> Color(0xFF2196F3)
    Category.EPARGNE               -> Color(0xFF4CAF50)
    Category.ENFANT                -> Color(0xFFFF9800)
    Category.EDUCATION             -> Color(0xFF9C27B0)
    Category.HABILLEMENT           -> Color(0xFF00BCD4)
    Category.IMPOTS_CHARGES        -> Color(0xFF607D8B)
    Category.ASSURANCES            -> Color(0xFF3F51B5)
    Category.TRANSFERTS            -> Color(0xFFFF5722)
    Category.TRANSFERTS_FAMILIAUX  -> Color(0xFF8BC34A)
    Category.TABAC                 -> Color(0xFF8D6E63) // brun, distinct du gris réservé à AUTRE
    Category.AUTRE                 -> Color(0xFF9E9E9E)
}

fun Category.chartIcon(): ImageVector = when (this) {
    Category.ALIMENTATION          -> Icons.Filled.ShoppingCart
    Category.LOGEMENT              -> Icons.Filled.Home
    Category.TRANSPORT             -> Icons.Filled.DirectionsBus
    Category.SANTE                 -> Icons.Filled.LocalHospital
    Category.LOISIRS               -> Icons.Filled.SportsEsports
    Category.ABONNEMENTS           -> Icons.Filled.Subscriptions
    Category.INVESTISSEMENT        -> Icons.AutoMirrored.Filled.TrendingUp
    Category.EPARGNE               -> Icons.Filled.Savings
    Category.ENFANT                -> Icons.Filled.ChildCare
    Category.EDUCATION             -> Icons.Filled.School
    Category.HABILLEMENT           -> Icons.Filled.Checkroom
    Category.IMPOTS_CHARGES        -> Icons.Filled.AccountBalance
    Category.ASSURANCES            -> Icons.Filled.Shield
    Category.TRANSFERTS            -> Icons.Filled.SwapHoriz
    Category.TRANSFERTS_FAMILIAUX  -> Icons.Filled.Groups
    Category.TABAC                 -> Icons.Filled.SmokingRooms
    Category.AUTRE                 -> Icons.Filled.MoreHoriz
}

/** Couleur et icône stables par [BankProvider], même logique que [Category.chartColor]/[chartIcon]. */
fun BankProvider.chartColor(): Color = when (this) {
    BankProvider.BRED           -> Color(0xFF1B5E20) // vert BRED
    BankProvider.TRADE_REPUBLIC -> Color(0xFF37474F) // gris ardoise
    BankProvider.ESPECES        -> Color(0xFF4CAF50)
    BankProvider.AUTRE          -> Color(0xFF9E9E9E)
}

fun BankProvider.chartIcon(): ImageVector = when (this) {
    BankProvider.BRED           -> Icons.Filled.AccountBalance
    BankProvider.TRADE_REPUBLIC -> Icons.AutoMirrored.Filled.TrendingUp
    BankProvider.ESPECES        -> Icons.Filled.Payments
    BankProvider.AUTRE          -> Icons.Filled.MoreHoriz
}
