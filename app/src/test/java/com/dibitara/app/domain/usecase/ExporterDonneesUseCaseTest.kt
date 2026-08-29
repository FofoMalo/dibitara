package com.dibitara.app.domain.usecase

import android.net.Uri
import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CategoryEnvelope
import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Debt
import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.model.DebtType
import com.dibitara.app.domain.model.ExportData
import com.dibitara.app.domain.model.ExportFormat
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.model.SavingsType
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.model.VehicleEntryType
import com.dibitara.app.domain.model.VehicleRentalEntry
import com.dibitara.app.domain.repository.BankAccountRepository
import com.dibitara.app.domain.repository.BudgetRepository
import com.dibitara.app.domain.repository.CategorizationRuleRepository
import com.dibitara.app.domain.repository.CategoryEnvelopeRepository
import com.dibitara.app.domain.repository.ChildRepository
import com.dibitara.app.domain.repository.CustomInvestmentRepository
import com.dibitara.app.domain.repository.CustomSubCategoryRepository
import com.dibitara.app.domain.repository.DebtRepository
import com.dibitara.app.domain.repository.ExportRepository
import com.dibitara.app.domain.repository.InvestmentRepository
import com.dibitara.app.domain.repository.SavingsRepository
import com.dibitara.app.domain.repository.TransactionRepository
import com.dibitara.app.domain.repository.VersementRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ExporterDonneesUseCaseTest {

    private val transactionRepo    : TransactionRepository      = mockk()
    private val budgetRepo         : BudgetRepository           = mockk()
    private val savingsRepo        : SavingsRepository          = mockk()
    private val investmentRepo     : InvestmentRepository       = mockk()
    private val debtRepo           : DebtRepository             = mockk()
    private val customInvestRepo   : CustomInvestmentRepository = mockk()
    private val childRepo          : ChildRepository            = mockk()
    private val subCategoryRepo    : CustomSubCategoryRepository = mockk()
    private val bankAccountRepo    : BankAccountRepository       = mockk()
    private val envelopeRepo       : CategoryEnvelopeRepository  = mockk()
    private val ruleRepo           : CategorizationRuleRepository = mockk()
    private val versementRepo      : VersementRepository         = mockk()
    private val exportRepo         : ExportRepository           = mockk()

    private lateinit var useCase: ExporterDonneesUseCase

    private val uriMock: Uri = mockk()

    @BeforeEach
    fun setUp() {
        useCase = ExporterDonneesUseCase(
            transactionRepository      = transactionRepo,
            budgetRepository           = budgetRepo,
            savingsRepository          = savingsRepo,
            investmentRepository       = investmentRepo,
            debtRepository             = debtRepo,
            customInvestmentRepository = customInvestRepo,
            childRepository            = childRepo,
            customSubCategoryRepository = subCategoryRepo,
            bankAccountRepository      = bankAccountRepo,
            categoryEnvelopeRepository = envelopeRepo,
            categorizationRuleRepository = ruleRepo,
            versementRepository        = versementRepo,
            exportRepository           = exportRepo
        )

        // Tous les repos retournent des listes vides par défaut
        coEvery { subCategoryRepo.getAll()                   } returns flowOf(emptyList())
        coEvery { bankAccountRepo.getAll()                   } returns flowOf(emptyList())
        coEvery { envelopeRepo.getAll()                      } returns flowOf(emptyList())
        coEvery { ruleRepo.getAll()                          } returns emptyList()
        coEvery { versementRepo.getAll()                     } returns emptyList()
        coEvery { childRepo.getAll()                         } returns flowOf(emptyList())
        coEvery { transactionRepo.getAll()                   } returns flowOf(emptyList())
        coEvery { budgetRepo.getAll()                        } returns flowOf(emptyList())
        coEvery { savingsRepo.getAll()                       } returns flowOf(emptyList())
        coEvery { investmentRepo.getAllRealEstate()           } returns flowOf(emptyList())
        coEvery { investmentRepo.getAllScpi()                 } returns flowOf(emptyList())
        coEvery { investmentRepo.getAllAirbnbRentals()        } returns flowOf(emptyList())
        coEvery { investmentRepo.getAllVehicleRentalEntries() } returns flowOf(emptyList())
        coEvery { debtRepo.getAll()                          } returns flowOf(emptyList())
        coEvery { customInvestRepo.getAllCustomAssets()       } returns flowOf(emptyList())
        coEvery { customInvestRepo.getAllEmployeeSavings()    } returns flowOf(emptyList())
        coEvery { exportRepo.exporter(any(), any())          } returns uriMock
    }

    @Test
    fun `retourne l'Uri produit par ExportRepository`() = runTest {
        val resultat = useCase(ExportFormat.CSV)
        assertEquals(uriMock, resultat)
    }

    @Test
    fun `transmet le format CSV à ExportRepository`() = runTest {
        useCase(ExportFormat.CSV)
        coVerify { exportRepo.exporter(any(), ExportFormat.CSV) }
    }

    @Test
    fun `transmet le format JSON à ExportRepository`() = runTest {
        useCase(ExportFormat.JSON)
        coVerify { exportRepo.exporter(any(), ExportFormat.JSON) }
    }

    @Test
    fun `agrège les données de tous les repositories`() = runTest {
        val transaction = Transaction(
            id = 1L, amountCents = 5000L, currency = Currency.EUR,
            category = Category.ALIMENTATION, type = TransactionType.EXPENSE,
            date = LocalDate.of(2026, 5, 22)
        )
        val budget = Budget(id = 1L, month = 5, year = 2026, allocatedCents = 200000L, spentCents = 5000L, currency = Currency.EUR)
        val compte = SavingsAccount(id = 1L, type = SavingsType.LIVRET_A, label = "Livret A", currentBalanceCents = 100000L, monthlyContributionCents = 5000L, currency = Currency.EUR, updatedAt = LocalDate.now())
        val immo   = RealEstateAsset(id = 1L, label = "Appart", currentValueCents = 15000000L, currency = Currency.EUR, updatedAt = LocalDate.now())
        val scpi   = ScpiInvestment(id = 1L, label = "SCPI X", sharesCount = 2.5, shareValueCents = 100000L, monthlyContributionCents = 5000L, currency = Currency.EUR, updatedAt = LocalDate.now())
        val airbnb = AirbnbRental(id = 1L, propertyLabel = "Studio", amountCents = 80000L, date = LocalDate.now(), currency = Currency.EUR)
        val vehicule = VehicleRentalEntry(id = 1L, label = "Location weekend", entryType = VehicleEntryType.REVENU, amountCents = 15000L, date = LocalDate.now(), currency = Currency.EUR)
        val dette  = Debt(id = 1L, label = "Crédit", totalCents = 500000L, monthlyPaymentCents = 50000L, currency = Currency.EUR, type = DebtType.CREDIT_IMMO, updatedAt = LocalDate.now())

        coEvery { transactionRepo.getAll()            } returns flowOf(listOf(transaction))
        coEvery { budgetRepo.getAll()                 } returns flowOf(listOf(budget))
        coEvery { savingsRepo.getAll()                } returns flowOf(listOf(compte))
        coEvery { investmentRepo.getAllRealEstate()   } returns flowOf(listOf(immo))
        coEvery { investmentRepo.getAllScpi()         } returns flowOf(listOf(scpi))
        coEvery { investmentRepo.getAllAirbnbRentals()} returns flowOf(listOf(airbnb))
        coEvery { investmentRepo.getAllVehicleRentalEntries() } returns flowOf(listOf(vehicule))
        coEvery { debtRepo.getAll()                  } returns flowOf(listOf(dette))

        useCase(ExportFormat.JSON)

        // Vérifie que l'ExportData transmis contient bien les données collectées
        coVerify {
            exportRepo.exporter(
                match { data ->
                    data.transactions.size == 1 &&
                    data.budgets.size      == 1 &&
                    data.epargne.size      == 1 &&
                    data.immobilier.size   == 1 &&
                    data.scpi.size         == 1 &&
                    data.airbnb.size       == 1 &&
                    data.vehiculeLocatif.size == 1 &&
                    data.dettes.size       == 1
                },
                ExportFormat.JSON
            )
        }
    }

    @Test
    fun `agrège aussi les versements mensuels et les enveloppes (régression sauvegarde incomplète)`() = runTest {
        val versement = MonthlyVersement(
            id = 1L, accountId = 1L, compteType = CompteType.EPARGNE,
            year = 2026, month = 8, montantCents = 20000L, currency = Currency.EUR
        )
        val enveloppe = CategoryEnvelope(id = 1L, category = Category.ALIMENTATION, plafondCents = 40000L, currency = Currency.EUR)
        coEvery { versementRepo.getAll() } returns listOf(versement)
        coEvery { envelopeRepo.getAll()  } returns flowOf(listOf(enveloppe))

        useCase(ExportFormat.JSON)

        coVerify {
            exportRepo.exporter(
                match { data -> data.versementsMensuels.size == 1 && data.enveloppesBudget.size == 1 },
                ExportFormat.JSON
            )
        }
    }
}
