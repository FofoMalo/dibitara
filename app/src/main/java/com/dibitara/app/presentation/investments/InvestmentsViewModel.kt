package com.dibitara.app.presentation.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.math.roundToLong
import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.CustomAsset
import com.dibitara.app.domain.model.EmployeeSavings
import com.dibitara.app.domain.model.EmployeeSavingsType
import com.dibitara.app.domain.model.ExchangeRates
import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.model.VehicleEntryType
import com.dibitara.app.domain.model.VehicleRentalEntry
import com.dibitara.app.domain.usecase.CalculerTendancePatrimoineUseCase
import com.dibitara.app.domain.usecase.DeleteAirbnbRentalUseCase
import com.dibitara.app.domain.usecase.DeleteVehicleRentalEntryUseCase
import com.dibitara.app.domain.usecase.DeleteCustomAssetUseCase
import com.dibitara.app.domain.usecase.DeleteEmployeeSavingsUseCase
import com.dibitara.app.domain.usecase.DeleteRealEstateUseCase
import com.dibitara.app.domain.usecase.DeleteScpiUseCase
import com.dibitara.app.domain.usecase.ExisteVersementMoisUseCase
import com.dibitara.app.domain.usecase.GetAirbnbRentalsByYearUseCase
import com.dibitara.app.domain.usecase.GetCustomAssetsUseCase
import com.dibitara.app.domain.usecase.GetEmployeeSavingsUseCase
import com.dibitara.app.domain.usecase.GetRealEstateUseCase
import com.dibitara.app.domain.usecase.GetScpiUseCase
import com.dibitara.app.domain.usecase.GetVehicleRentalEntriesUseCase
import com.dibitara.app.domain.model.CurrencyConverter
import com.dibitara.app.domain.model.Debt
import com.dibitara.app.domain.repository.ExchangeRateRepository
import com.dibitara.app.domain.usecase.GetDebtsUseCase
import com.dibitara.app.domain.usecase.GetPatrimoineHistoryUseCase
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.SaveAirbnbRentalUseCase
import com.dibitara.app.domain.usecase.SaveCustomAssetUseCase
import com.dibitara.app.domain.usecase.SaveEmployeeSavingsUseCase
import com.dibitara.app.domain.usecase.SaveRealEstateUseCase
import com.dibitara.app.domain.usecase.SaveScpiUseCase
import com.dibitara.app.domain.usecase.SaveVehicleRentalEntryUseCase
import com.dibitara.app.domain.usecase.SaveVersementUseCase
import com.dibitara.app.domain.usecase.UpdateAirbnbRentalUseCase
import com.dibitara.app.domain.usecase.UpdateCustomAssetUseCase
import com.dibitara.app.domain.usecase.UpdateEmployeeSavingsUseCase
import com.dibitara.app.domain.usecase.UpdateRealEstateUseCase
import com.dibitara.app.domain.usecase.UpdateScpiUseCase
import com.dibitara.app.domain.usecase.UpdateVehicleRentalEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// Chaque UseCase est préfixé "uc" pour éviter toute ambiguïté avec les méthodes publiques du même nom.
@HiltViewModel
class InvestmentsViewModel @Inject constructor(
    private val ucGetRealEstate: GetRealEstateUseCase,
    private val ucGetScpi: GetScpiUseCase,
    private val ucGetAirbnbByYear: GetAirbnbRentalsByYearUseCase,
    private val ucGetVehicleRentals: GetVehicleRentalEntriesUseCase,
    private val ucGetCustomAssets: GetCustomAssetsUseCase,
    private val ucGetEmployeeSavings: GetEmployeeSavingsUseCase,
    private val ucSaveRealEstate: SaveRealEstateUseCase,
    private val ucSaveScpi: SaveScpiUseCase,
    private val ucSaveAirbnbRental: SaveAirbnbRentalUseCase,
    private val ucSaveVehicleEntry: SaveVehicleRentalEntryUseCase,
    private val ucSaveCustomAsset: SaveCustomAssetUseCase,
    private val ucSaveEmployeeSavings: SaveEmployeeSavingsUseCase,
    private val ucUpdateRealEstate: UpdateRealEstateUseCase,
    private val ucUpdateScpi: UpdateScpiUseCase,
    private val ucUpdateAirbnbRental: UpdateAirbnbRentalUseCase,
    private val ucUpdateVehicleEntry: UpdateVehicleRentalEntryUseCase,
    private val ucUpdateCustomAsset: UpdateCustomAssetUseCase,
    private val ucUpdateEmployeeSavings: UpdateEmployeeSavingsUseCase,
    private val ucDeleteRealEstate: DeleteRealEstateUseCase,
    private val ucDeleteScpi: DeleteScpiUseCase,
    private val ucDeleteAirbnbRental: DeleteAirbnbRentalUseCase,
    private val ucDeleteVehicleEntry: DeleteVehicleRentalEntryUseCase,
    private val ucDeleteCustomAsset: DeleteCustomAssetUseCase,
    private val ucDeleteEmployeeSavings: DeleteEmployeeSavingsUseCase,
    private val ucSaveVersement: SaveVersementUseCase,
    private val ucExisteVersementMois: ExisteVersementMoisUseCase,
    private val ucGetPreferences: GetUserPreferencesUseCase,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val ucGetDebts: GetDebtsUseCase,
    private val ucGetPatrimoineHistory: GetPatrimoineHistoryUseCase,
    private val ucCalculerTendance: CalculerTendancePatrimoineUseCase
) : ViewModel() {

    val defaultCurrency: StateFlow<Currency> = ucGetPreferences()
        .map { it.deviseParDefaut }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.EUR)

    private val currentYear = LocalDate.now().year

    private val baseFlow = combine(
        ucGetRealEstate(),
        ucGetScpi(),
        ucGetAirbnbByYear(currentYear),
        ucGetDebts(),
        // Cumulé depuis le début de l'activité, pas filtré par année (une grosse charge
        // initiale peut précéder les revenus qu'elle a permis de générer).
        ucGetVehicleRentals()
    ) { realEstate, scpi, airbnb, debts, vehicleEntries ->
        BaseData(realEstate, scpi, airbnb, debts, vehicleEntries)
    }

    private val customFlow = combine(
        ucGetCustomAssets(),
        ucGetEmployeeSavings()
    ) { assets, empSavings -> assets to empSavings }

    // Flux de conversion : devise cible + taux en cache
    private val conversionFlow = combine(
        ucGetPreferences(),
        exchangeRateRepository.getRatesFlow()
    ) { prefs, rates -> prefs.deviseParDefaut to rates }

    val uiState: StateFlow<InvestmentsUiState> = combine(baseFlow, customFlow, conversionFlow) {
        base, (assets, empSavings), (target, rates) ->
        // Conversion de chaque actif vers la devise par défaut avant sommation
        fun Long.cvt(from: com.dibitara.app.domain.model.Currency) =
            CurrencyConverter.convertCents(this, from, target, rates)
        InvestmentsUiState.Success(
            realEstate            = base.realEstate,
            scpi                  = base.scpi,
            airbnbRentals         = base.airbnb,
            airbnbAnnualTotal     = base.airbnb.sumOf { it.amountCents.cvt(it.currency) },
            anneeLocatifs         = currentYear,
            vehicleRentalEntries       = base.vehicleEntries,
            vehicleRentalRevenueCents  = base.vehicleEntries
                .filter { it.entryType == VehicleEntryType.REVENU }
                .sumOf { it.amountCents.cvt(it.currency) },
            vehicleRentalChargeCents   = base.vehicleEntries
                .filter { it.entryType == VehicleEntryType.CHARGE }
                .sumOf { it.amountCents.cvt(it.currency) },
            customAssets          = assets,
            employeeSavings       = empSavings,
            availableDebts        = base.debts,
            totalInvestmentsCents =
                base.realEstate.sumOf { it.currentValueCents.cvt(it.currency) } +
                base.scpi.sumOf       { it.totalValueCents.cvt(it.currency) }   +
                assets.sumOf          { it.totalValueCents.cvt(it.currency) }   +
                empSavings.sumOf      { it.currentBalanceCents.cvt(it.currency) },
            summaryCurrency       = target,
            rates                 = rates
        ) as InvestmentsUiState
    }
        .combine(ucGetPatrimoineHistory()) { state, history ->
            if (state is InvestmentsUiState.Success)
                state.copy(patrimoineTrendPct = ucCalculerTendance(history))
            else state
        }
        .catch { emit(InvestmentsUiState.Error(it.message ?: "Erreur inconnue")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InvestmentsUiState.Loading
        )

    private val _event = MutableSharedFlow<InvestmentsEvent>()
    val event: SharedFlow<InvestmentsEvent> = _event.asSharedFlow()

    fun addRealEstate(label: String, valueStr: String, currency: Currency, debtId: Long? = null) {
        val cents = valueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Montant invalide")) }
            return
        }
        viewModelScope.launch {
            ucSaveRealEstate(
                RealEstateAsset(label = label, currentValueCents = cents, currency = currency,
                    updatedAt = LocalDate.now(), debtId = debtId)
            )
                .onSuccess { _event.emit(InvestmentsEvent.Saved) }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun addScpi(label: String, sharesStr: String, shareValueStr: String, contributionStr: String, currency: Currency) {
        val shares = sharesStr.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Nombre de parts invalide")) }
            return
        }
        val shareValue = shareValueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
        val contribution = contributionStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
        viewModelScope.launch {
            ucSaveScpi(
                ScpiInvestment(
                    label = label,
                    sharesCount = shares,
                    shareValueCents = shareValue,
                    monthlyContributionCents = contribution,
                    currency = currency,
                    updatedAt = LocalDate.now()
                )
            )
                .onSuccess { _event.emit(InvestmentsEvent.Saved) }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun addAirbnbRental(label: String, amountStr: String, date: LocalDate, currency: Currency) {
        val cents = amountStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Montant invalide")) }
            return
        }
        viewModelScope.launch {
            ucSaveAirbnbRental(
                AirbnbRental(propertyLabel = label, amountCents = cents, date = date, currency = currency)
            )
                .onSuccess { _event.emit(InvestmentsEvent.Saved) }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun updateRealEstate(asset: RealEstateAsset, label: String, valueStr: String, currency: Currency, debtId: Long? = null) {
        val cents = valueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Montant invalide")) }
            return
        }
        viewModelScope.launch {
            ucUpdateRealEstate(asset.copy(label = label, currentValueCents = cents, currency = currency,
                updatedAt = LocalDate.now(), debtId = debtId))
                .onSuccess { _event.emit(InvestmentsEvent.Saved) }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun updateScpi(scpi: ScpiInvestment, label: String, sharesStr: String, shareValueStr: String, contributionStr: String, currency: Currency) {
        val shares = sharesStr.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Nombre de parts invalide")) }
            return
        }
        val shareValue   = shareValueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
        val contribution = contributionStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
        viewModelScope.launch {
            ucUpdateScpi(scpi.copy(label = label, sharesCount = shares, shareValueCents = shareValue, monthlyContributionCents = contribution, currency = currency, updatedAt = LocalDate.now()))
                .onSuccess { _event.emit(InvestmentsEvent.Saved) }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun updateAirbnbRental(rental: AirbnbRental, label: String, amountStr: String, currency: Currency) {
        val cents = amountStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Montant invalide")) }
            return
        }
        viewModelScope.launch {
            ucUpdateAirbnbRental(rental.copy(propertyLabel = label, amountCents = cents, currency = currency))
                .onSuccess { _event.emit(InvestmentsEvent.Saved) }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun deleteRealEstate(asset: RealEstateAsset) {
        viewModelScope.launch { ucDeleteRealEstate(asset) }
    }

    fun deleteScpi(scpi: ScpiInvestment) {
        viewModelScope.launch { ucDeleteScpi(scpi) }
    }

    fun deleteAirbnb(rental: AirbnbRental) {
        viewModelScope.launch { ucDeleteAirbnbRental(rental) }
    }

    // ─── Véhicule locatif ──────────────────────────────────────────────────────

    fun addVehicleRentalEntry(label: String, amountStr: String, type: VehicleEntryType, date: LocalDate, currency: Currency) {
        val cents = amountStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Montant invalide")) }
            return
        }
        viewModelScope.launch {
            ucSaveVehicleEntry(
                VehicleRentalEntry(label = label, entryType = type, amountCents = cents, date = date, currency = currency)
            )
                .onSuccess { _event.emit(InvestmentsEvent.Saved) }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun updateVehicleRentalEntry(entry: VehicleRentalEntry, label: String, amountStr: String, type: VehicleEntryType, date: LocalDate, currency: Currency) {
        val cents = amountStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Montant invalide")) }
            return
        }
        viewModelScope.launch {
            ucUpdateVehicleEntry(entry.copy(label = label, entryType = type, amountCents = cents, date = date, currency = currency))
                .onSuccess { _event.emit(InvestmentsEvent.Saved) }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun deleteVehicleRentalEntry(entry: VehicleRentalEntry) {
        viewModelScope.launch { ucDeleteVehicleEntry(entry) }
    }

    // ─── Actifs libres ─────────────────────────────────────────────────────────

    fun addCustomAsset(label: String, valueStr: String, currency: Currency) {
        val cents = valueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Montant invalide")) }
            return
        }
        viewModelScope.launch {
            ucSaveCustomAsset(CustomAsset(label = label, totalValueCents = cents, currency = currency, updatedAt = LocalDate.now()))
                .onSuccess { _event.emit(InvestmentsEvent.Saved) }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun updateCustomAsset(asset: CustomAsset, label: String, valueStr: String, currency: Currency) {
        val cents = valueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Montant invalide")) }
            return
        }
        viewModelScope.launch {
            ucUpdateCustomAsset(asset.copy(label = label, totalValueCents = cents, currency = currency, updatedAt = LocalDate.now()))
                .onSuccess { _event.emit(InvestmentsEvent.Saved) }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun deleteCustomAsset(asset: CustomAsset) {
        viewModelScope.launch { ucDeleteCustomAsset(asset) }
    }

    // ─── Épargne salariale ─────────────────────────────────────────────────────

    fun addEmployeeSavings(type: EmployeeSavingsType, label: String, balanceStr: String, contributionStr: String, currency: Currency) {
        val balance = balanceStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Solde invalide")) }
            return
        }
        val contribution = contributionStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
        viewModelScope.launch {
            ucSaveEmployeeSavings(EmployeeSavings(type = type, label = label, currentBalanceCents = balance, employerContributionCents = contribution, currency = currency, updatedAt = LocalDate.now()))
                .onSuccess { _event.emit(InvestmentsEvent.Saved) }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun updateEmployeeSavings(savings: EmployeeSavings, type: EmployeeSavingsType, label: String, balanceStr: String, contributionStr: String, currency: Currency) {
        val balance = balanceStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Solde invalide")) }
            return
        }
        val contribution = contributionStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
        viewModelScope.launch {
            ucUpdateEmployeeSavings(savings.copy(type = type, label = label, currentBalanceCents = balance, employerContributionCents = contribution, currency = currency, updatedAt = LocalDate.now()))
                .onSuccess { _event.emit(InvestmentsEvent.Saved) }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun deleteEmployeeSavings(savings: EmployeeSavings) {
        viewModelScope.launch { ucDeleteEmployeeSavings(savings) }
    }

    /**
     * Applique le versement mensuel prévu sur une SCPI.
     * Même logique que pour l'épargne : non-rétroactif, un seul versement par mois.
     */
    fun appliquerVersementScpi(scpi: ScpiInvestment) {
        val now = LocalDate.now()
        viewModelScope.launch {
            if (ucExisteVersementMois(scpi.id, CompteType.SCPI, now.year, now.monthValue)) {
                _event.emit(InvestmentsEvent.Error("Versement déjà enregistré pour ce mois"))
                return@launch
            }
            val versement = MonthlyVersement(
                accountId    = scpi.id,
                compteType   = CompteType.SCPI,
                year         = now.year,
                month        = now.monthValue,
                montantCents = scpi.monthlyContributionCents,
                currency     = scpi.currency
            )
            ucSaveVersement(versement)
                .onSuccess {
                    ucUpdateScpi(
                        scpi.copy(
                            sharesCount = scpi.sharesCount,
                            shareValueCents = scpi.shareValueCents,
                            monthlyContributionCents = scpi.monthlyContributionCents,
                            updatedAt = now
                        )
                    )
                    _event.emit(InvestmentsEvent.VersementApplique)
                }
                .onFailure { _event.emit(InvestmentsEvent.Error("Vérifier les informations saisies")) }
        }
    }
}

sealed class InvestmentsUiState {
    data object Loading : InvestmentsUiState()
    data class Success(
        val realEstate            : List<RealEstateAsset>,
        val scpi                  : List<ScpiInvestment>,
        val airbnbRentals         : List<AirbnbRental>,
        val airbnbAnnualTotal     : Long,
        val anneeLocatifs         : Int,
        val vehicleRentalEntries      : List<VehicleRentalEntry> = emptyList(),
        val vehicleRentalRevenueCents : Long = 0L,
        val vehicleRentalChargeCents  : Long = 0L,
        val customAssets          : List<CustomAsset>        = emptyList(),
        val employeeSavings       : List<EmployeeSavings>    = emptyList(),
        val availableDebts        : List<Debt>               = emptyList(),
        val totalInvestmentsCents : Long                     = 0L,
        val summaryCurrency       : Currency                 = Currency.EUR,
        val rates                 : ExchangeRates             = ExchangeRates(usdParEur = 1.0, xofParEur = 1.0, horodatage = 0L),
        val patrimoineTrendPct    : Float?                   = null
    ) : InvestmentsUiState()
    data class Error(val message: String) : InvestmentsUiState()
}

// Holder interne : contourne la limite de 5 arguments de combine()
private data class BaseData(
    val realEstate     : List<RealEstateAsset>,
    val scpi           : List<ScpiInvestment>,
    val airbnb         : List<AirbnbRental>,
    val debts          : List<Debt>,
    val vehicleEntries : List<VehicleRentalEntry>
)

sealed class InvestmentsEvent {
    data object Saved : InvestmentsEvent()
    data object VersementApplique : InvestmentsEvent()
    data class Error(val message: String) : InvestmentsEvent()
}
