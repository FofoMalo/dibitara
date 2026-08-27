package com.dibitara.app.presentation.dashboard

import com.dibitara.app.domain.model.BankAccountsSummary
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CashflowProjection
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.DashboardCard
import com.dibitara.app.domain.model.MonthlyReport
import com.dibitara.app.domain.model.PatrimonyOverview
import com.dibitara.app.domain.model.RecategorizationSuggestion
import com.dibitara.app.domain.model.SubCategory
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.model.UserPreferences
import com.dibitara.app.domain.usecase.CalculerTendancePatrimoineUseCase
import com.dibitara.app.domain.usecase.GetBankAccountsSummaryUseCase
import com.dibitara.app.domain.usecase.GetCashflowProjectionUseCase
import com.dibitara.app.domain.usecase.GetEnveloppesEnAlerteUseCase
import com.dibitara.app.domain.usecase.GetMonthlyReportUseCase
import com.dibitara.app.domain.usecase.GetPatrimoineHistoryUseCase
import com.dibitara.app.domain.usecase.GetPatrimonyOverviewUseCase
import com.dibitara.app.domain.usecase.GetRecategorizationSuggestionsUseCase
import com.dibitara.app.domain.usecase.GetSpendingHistoryUseCase
import com.dibitara.app.domain.usecase.GetUpcomingPaymentsUseCase
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.UpdateDashboardCardOrderUseCase
import com.dibitara.app.domain.usecase.UpdateDeviseParDefautUseCase
import com.dibitara.app.domain.usecase.UpdateMasquerMontantsUseCase
import com.dibitara.app.domain.usecase.UpdateTransactionUseCase
import com.dibitara.app.domain.usecase.UpsertCategorizationRuleUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val getPatrimonyOverview   : GetPatrimonyOverviewUseCase        = mockk()
    private val getSpendingHistory     : GetSpendingHistoryUseCase         = mockk()
    private val getMonthlyReport       : GetMonthlyReportUseCase           = mockk()
    private val getUpcomingPayments    : GetUpcomingPaymentsUseCase        = mockk()
    private val getPreferences         : GetUserPreferencesUseCase         = mockk()
    private val getCashflowProjection  : GetCashflowProjectionUseCase      = mockk()
    private val getRecategorizations   : GetRecategorizationSuggestionsUseCase = mockk()
    private val getEnveloppesEnAlerte  : GetEnveloppesEnAlerteUseCase      = mockk()
    private val getBankAccountsSummary : GetBankAccountsSummaryUseCase     = mockk()
    private val getPatrimoineHistory   : GetPatrimoineHistoryUseCase       = mockk()
    private val calculerTendance       : CalculerTendancePatrimoineUseCase = mockk()
    private val updateTransaction      : UpdateTransactionUseCase          = mockk(relaxed = true)
    private val updateCardOrder        : UpdateDashboardCardOrderUseCase   = mockk(relaxed = true)
    private val ucUpsertRule           : UpsertCategorizationRuleUseCase   = mockk(relaxed = true)
    private val updateDevise           : UpdateDeviseParDefautUseCase      = mockk(relaxed = true)
    private val updateMasquerMontants  : UpdateMasquerMontantsUseCase      = mockk(relaxed = true)

    private lateinit var viewModel: DashboardViewModel

    private val today = LocalDate.now()

    private fun overview(liquidites: Long = 10_000L) = PatrimonyOverview(
        liquiditesCents = liquidites, epargneCents = 500_000L, investissementsCents = 2_000_000L,
        airbnbAnnualRevenueCents = 0L, vehicleRentalNetRevenueCents = 0L, dettesTotalCents = 0L,
        currency = Currency.EUR
    )

    private fun rapport() = MonthlyReport(
        month = today.monthValue, year = today.year, currency = Currency.EUR,
        revenusCents = 300_000L, depensesCents = 100_000L, soldeCents = 200_000L,
        budget = null, topCategories = emptyList(), variationDepensesCents = 0L
    )

    private fun cashflow() = CashflowProjection(
        soldeActuelCents = 100_000L, soldeProjecte30jCents = 90_000L,
        jourPassageSeuilNegatif = null, pointsTimeline = emptyList(), currency = Currency.EUR
    )

    private fun buildViewModel() = DashboardViewModel(
        getPatrimonyOverview, getSpendingHistory, getMonthlyReport, getUpcomingPayments,
        getPreferences, getCashflowProjection, getRecategorizations, getEnveloppesEnAlerte,
        getBankAccountsSummary, getPatrimoineHistory, calculerTendance, updateTransaction,
        updateCardOrder, ucUpsertRule, updateDevise, updateMasquerMontants
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getPatrimonyOverview(any(), any()) } returns flowOf(overview())
        every { getSpendingHistory() } returns flowOf(emptyList())
        every { getMonthlyReport(any(), any()) } returns flowOf(rapport())
        every { getUpcomingPayments(any()) } returns flowOf(emptyList())
        every { getPreferences() } returns flowOf(UserPreferences())
        every { getCashflowProjection() } returns flowOf(cashflow())
        every { getRecategorizations() } returns flowOf(emptyList())
        every { getEnveloppesEnAlerte(any()) } returns flowOf(emptyList())
        every { getBankAccountsSummary() } returns flowOf(BankAccountsSummary(emptyList(), 0L, Currency.EUR))
        every { getPatrimoineHistory() } returns flowOf(emptyList())
        every { calculerTendance(any()) } returns null
        viewModel = buildViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState Success reprend les valeurs des sources`() = runTest {
        every { getPreferences() } returns flowOf(UserPreferences(afficherRapportMensuel = true))
        viewModel = buildViewModel()

        val state = viewModel.uiState.first { it is DashboardUiState.Success } as DashboardUiState.Success

        assertEquals(10_000L, state.overview.liquiditesCents)
        assertEquals(200_000L, state.rapportMensuel?.soldeCents)
        assertEquals(90_000L, state.cashflowProjection?.soldeProjecte30jCents)
        assertEquals(DashboardCard.entries.toList(), state.cardOrder)
    }

    @Test
    fun `erreur d'une source remonte en DashboardUiState Error`() = runTest {
        every { getPatrimonyOverview(any(), any()) } returns flow { throw RuntimeException("panne") }
        viewModel = buildViewModel()

        val state = viewModel.uiState.first { it !is DashboardUiState.Loading }

        assertTrue(state is DashboardUiState.Error)
    }

    @Test
    fun `afficherProchainsPaiements à false vide la liste malgré des paiements disponibles`() = runTest {
        every { getPreferences() } returns flowOf(UserPreferences(afficherProchainsPaiements = false))
        every { getUpcomingPayments(any()) } returns flowOf(listOf(
            com.dibitara.app.domain.model.UpcomingPayment(
                template = buildTransaction(), nextDate = today.plusDays(3)
            )
        ))
        viewModel = buildViewModel()

        val state = viewModel.uiState.first { it is DashboardUiState.Success } as DashboardUiState.Success

        assertTrue(state.upcomingPayments.isEmpty())
    }

    @Test
    fun `afficherRapportMensuel à false met rapportMensuel à null`() = runTest {
        every { getPreferences() } returns flowOf(UserPreferences(afficherRapportMensuel = false))
        viewModel = buildViewModel()

        val state = viewModel.uiState.first { it is DashboardUiState.Success } as DashboardUiState.Success

        assertNull(state.rapportMensuel)
    }

    @Test
    fun `toggleEditMode inverse isEditMode`() = runTest {
        assertFalse(viewModel.isEditMode.value)
        viewModel.toggleEditMode()
        assertTrue(viewModel.isEditMode.value)
        viewModel.toggleEditMode()
        assertFalse(viewModel.isEditMode.value)
    }

    @Test
    fun `changerDevise délègue à UpdateDeviseParDefautUseCase`() = runTest {
        viewModel.changerDevise(Currency.USD)
        coVerify { updateDevise(Currency.USD) }
    }

    @Test
    fun `toggleMasquerMontants inverse la préférence actuelle`() = runTest {
        every { getPreferences() } returns flowOf(UserPreferences(masquerMontants = true))
        viewModel = buildViewModel()
        viewModel.uiState.first { it is DashboardUiState.Success }

        viewModel.toggleMasquerMontants()

        coVerify { updateMasquerMontants(false) }
    }

    @Test
    fun `moveCard réordonne et persiste le nouvel ordre`() = runTest {
        viewModel.uiState.first { it is DashboardUiState.Success }
        val order = DashboardCard.entries.toList()

        viewModel.moveCard(order[2].name, order[0].name)

        coVerify { updateCardOrder(match { it[0] == order[2] }) }
    }

    @Test
    fun `moveCard ignore une clé inconnue`() = runTest {
        viewModel.uiState.first { it is DashboardUiState.Success }

        viewModel.moveCard("INCONNU", DashboardCard.entries.first().name)

        coVerify(exactly = 0) { updateCardOrder(any()) }
    }

    @Test
    fun `appliquerRecategorisation avec sous-catégorie ne change pas la catégorie principale`() = runTest {
        val tx = buildTransaction()
        val suggestion = RecategorizationSuggestion(
            transaction = tx, suggestedCategory = Category.AUTRE,
            matchedKeyword = "netflix", suggestedSubCategory = SubCategory.CADEAUX
        )

        viewModel.appliquerRecategorisation(suggestion)

        coVerify {
            updateTransaction(match { it.subCategory == SubCategory.CADEAUX && it.category == tx.category })
        }
        coVerify { ucUpsertRule(tx.note, tx.type, Category.AUTRE, SubCategory.CADEAUX) }
    }

    @Test
    fun `appliquerRecategorisation sans sous-catégorie change la catégorie et efface la sous-catégorie`() = runTest {
        val tx = buildTransaction()
        val suggestion = RecategorizationSuggestion(
            transaction = tx, suggestedCategory = Category.ALIMENTATION, matchedKeyword = "carrefour"
        )

        viewModel.appliquerRecategorisation(suggestion)

        coVerify {
            updateTransaction(match { it.category == Category.ALIMENTATION && it.subCategory == null })
        }
    }

    @Test
    fun `refuserRecategorisation pose subCategory DIVERS sans changer la catégorie`() = runTest {
        val tx = buildTransaction()
        val suggestion = RecategorizationSuggestion(
            transaction = tx, suggestedCategory = Category.ALIMENTATION, matchedKeyword = "carrefour"
        )

        viewModel.refuserRecategorisation(suggestion)

        coVerify {
            updateTransaction(match { it.subCategory == SubCategory.DIVERS && it.category == tx.category })
        }
    }

    private fun buildTransaction() = Transaction(
        id = 1L, amountCents = 1500L, currency = Currency.EUR,
        category = Category.AUTRE, type = TransactionType.EXPENSE,
        date = today, note = "Netflix"
    )
}
