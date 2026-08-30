package com.dibitara.app.presentation.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.math.roundToLong
import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.AssetValuationType
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
import com.dibitara.app.domain.usecase.CalculerPerformanceActifUseCase
import com.dibitara.app.domain.usecase.CalculerTendanceActifUseCase
import com.dibitara.app.domain.usecase.CalculerTendancePatrimoineUseCase
import com.dibitara.app.domain.usecase.DeleteAirbnbRentalUseCase
import com.dibitara.app.domain.usecase.DeleteVehicleRentalEntryUseCase
import com.dibitara.app.domain.usecase.DeleteCustomAssetUseCase
import com.dibitara.app.domain.usecase.DeleteEmployeeSavingsUseCase
import com.dibitara.app.domain.usecase.DeleteRealEstateUseCase
import com.dibitara.app.domain.usecase.DeleteScpiUseCase
import com.dibitara.app.domain.usecase.EnregistrerMouvementCapitalUseCase
import com.dibitara.app.domain.usecase.ExisteVersementMoisUseCase
import com.dibitara.app.domain.usecase.GetAirbnbRentalsByYearUseCase
import com.dibitara.app.domain.usecase.GetCustomAssetsUseCase
import com.dibitara.app.domain.usecase.GetEmployeeSavingsUseCase
import com.dibitara.app.domain.usecase.GetRealEstateUseCase
import com.dibitara.app.domain.usecase.GetScpiUseCase
import com.dibitara.app.domain.usecase.GetVehicleRentalEntriesUseCase
import com.dibitara.app.domain.usecase.PerformanceActif
import com.dibitara.app.domain.model.CurrencyConverter
import com.dibitara.app.domain.model.Debt
import com.dibitara.app.domain.repository.ExchangeRateRepository
import com.dibitara.app.domain.usecase.GetAssetValuationHistoryUseCase
import com.dibitara.app.domain.usecase.GetDebtsUseCase
import com.dibitara.app.domain.usecase.GetPatrimoineHistoryUseCase
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.SaveAirbnbRentalUseCase
import com.dibitara.app.domain.usecase.SaveAssetValuationSnapshotUseCase
import com.dibitara.app.domain.usecase.SaveCustomAssetUseCase
import com.dibitara.app.domain.usecase.SaveEmployeeSavingsUseCase
import com.dibitara.app.domain.usecase.SaveRealEstateUseCase
import com.dibitara.app.domain.usecase.SaveScpiUseCase
import com.dibitara.app.domain.usecase.SaveVehicleRentalEntryUseCase
import com.dibitara.app.domain.usecase.SaveVersementUseCase
import com.dibitara.app.domain.usecase.SommeVersementsDepuisUseCase
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
    private val ucCalculerTendance: CalculerTendancePatrimoineUseCase,
    private val ucSaveAssetSnapshot: SaveAssetValuationSnapshotUseCase,
    private val ucGetAssetValuationHistory: GetAssetValuationHistoryUseCase,
    private val ucCalculerTendanceActif: CalculerTendanceActifUseCase,
    private val ucCalculerPerformanceActif: CalculerPerformanceActifUseCase,
    private val ucSommeVersementsDepuis: SommeVersementsDepuisUseCase,
    private val ucEnregistrerMouvementCapital: EnregistrerMouvementCapitalUseCase
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
        // Conversion de chaque actif vers la devise par défaut avant sommation.
        // hasConversion n'est lu qu'après avoir calculé tous les totaux ci-dessous,
        // dans des val intermédiaires - voir GetPatrimonyOverviewUseCase pour la
        // même remarque sur pourquoi ne pas imbriquer les .cvt() dans le constructeur.
        var hasConversion = false
        fun Long.cvt(from: com.dibitara.app.domain.model.Currency): Long {
            if (!CurrencyConverter.isSameCurrency(from, target)) hasConversion = true
            return CurrencyConverter.convertCents(this, from, target, rates)
        }

        val airbnbAnnualTotal = base.airbnb.sumOf { it.amountCents.cvt(it.currency) }
        val vehicleRentalRevenueCents = base.vehicleEntries
            .filter { it.entryType == VehicleEntryType.REVENU }
            .sumOf { it.amountCents.cvt(it.currency) }
        val vehicleRentalChargeCents = base.vehicleEntries
            .filter { it.entryType == VehicleEntryType.CHARGE }
            .sumOf { it.amountCents.cvt(it.currency) }
        val totalInvestmentsCents =
            base.realEstate.sumOf { it.currentValueCents.cvt(it.currency) } +
            base.scpi.sumOf       { it.totalValueCents.cvt(it.currency) }   +
            assets.sumOf          { it.totalValueCents.cvt(it.currency) }   +
            empSavings.sumOf      { it.currentBalanceCents.cvt(it.currency) }

        InvestmentsUiState.Success(
            realEstate            = base.realEstate,
            scpi                  = base.scpi,
            airbnbRentals         = base.airbnb,
            airbnbAnnualTotal     = airbnbAnnualTotal,
            anneeLocatifs         = currentYear,
            vehicleRentalEntries       = base.vehicleEntries,
            vehicleRentalRevenueCents  = vehicleRentalRevenueCents,
            vehicleRentalChargeCents   = vehicleRentalChargeCents,
            customAssets          = assets,
            employeeSavings       = empSavings,
            availableDebts        = base.debts,
            totalInvestmentsCents = totalInvestmentsCents,
            summaryCurrency       = target,
            rates                 = rates,
            hasConvertedValues    = hasConversion
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

    /**
     * Tendance d'un actif individuel (Immobilier/SCPI), lue à la demande plutôt que
     * câblée dans le flux réactif de [uiState] : l'historique de valorisation ne
     * change que sur une action d'édition explicite, pas en continu - un simple
     * appel ponctuel depuis la carte (voir InvestmentsScreen.kt) reste correct et
     * bien plus lisible qu'un flux combiné dynamiquement par actif.
     */
    suspend fun tendancePourActif(type: AssetValuationType, assetId: Long): Float? =
        ucCalculerTendanceActif(ucGetAssetValuationHistory(type, assetId).first())

    /**
     * Performance "Acquisition → Aujourd'hui" (voir [AcquisitionEvolutionBlock][com.dibitara.app.presentation.common.AcquisitionEvolutionBlock]),
     * nette des versements/abondements/mouvements de capital enregistrés depuis l'acquisition
     * pour les comptes qui en ont ([versementCompteType] non null - SCPI, épargne salariale,
     * actif libre et immobilier).
     */
    suspend fun performanceDepuisAcquisition(
        accountId: Long,
        acquisitionValueCents: Long,
        currentValueCents: Long,
        acquisitionDate: LocalDate,
        versementCompteType: CompteType? = null
    ): PerformanceActif? {
        val versementsCumules = versementCompteType?.let { ucSommeVersementsDepuis(accountId, it, acquisitionDate) } ?: 0L
        return ucCalculerPerformanceActif(acquisitionValueCents, currentValueCents, versementsCumules)
    }

    fun addRealEstate(
        label: String,
        valueStr: String,
        currency: Currency,
        debtId: Long? = null,
        acquisitionValueStr: String = "",
        acquisitionDate: LocalDate? = null
    ) {
        val cents = valueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Montant invalide")) }
            return
        }
        val acquisitionCents = acquisitionValueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() }
        viewModelScope.launch {
            ucSaveRealEstate(
                RealEstateAsset(label = label, currentValueCents = cents, currency = currency,
                    updatedAt = LocalDate.now(), debtId = debtId,
                    acquisitionValueCents = acquisitionCents, acquisitionDate = acquisitionDate)
            )
                .onSuccess { newId ->
                    ucSaveAssetSnapshot(AssetValuationType.REAL_ESTATE, newId, cents, currency)
                    _event.emit(InvestmentsEvent.Saved)
                }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun addScpi(
        label: String,
        sharesStr: String,
        shareValueStr: String,
        contributionStr: String,
        currency: Currency,
        acquisitionValueStr: String = "",
        acquisitionDate: LocalDate? = null
    ) {
        val shares = sharesStr.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Nombre de parts invalide")) }
            return
        }
        val shareValue = shareValueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
        val contribution = contributionStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
        val acquisitionCents = acquisitionValueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() }
        val scpiToSave = ScpiInvestment(
            label = label,
            sharesCount = shares,
            shareValueCents = shareValue,
            monthlyContributionCents = contribution,
            currency = currency,
            updatedAt = LocalDate.now(),
            acquisitionValueCents = acquisitionCents,
            acquisitionDate = acquisitionDate
        )
        viewModelScope.launch {
            ucSaveScpi(scpiToSave)
                .onSuccess { newId ->
                    ucSaveAssetSnapshot(AssetValuationType.SCPI, newId, scpiToSave.totalValueCents, currency)
                    _event.emit(InvestmentsEvent.Saved)
                }
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

    fun updateRealEstate(
        asset: RealEstateAsset,
        label: String,
        valueStr: String,
        currency: Currency,
        debtId: Long? = null,
        acquisitionValueStr: String = "",
        acquisitionDate: LocalDate? = null,
        mouvementCapitalCents: Long? = null
    ) {
        val cents = valueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Montant invalide")) }
            return
        }
        val acquisitionCents = acquisitionValueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() }
        viewModelScope.launch {
            ucUpdateRealEstate(asset.copy(label = label, currentValueCents = cents, currency = currency,
                updatedAt = LocalDate.now(), debtId = debtId,
                acquisitionValueCents = acquisitionCents, acquisitionDate = acquisitionDate))
                .onSuccess {
                    ucSaveAssetSnapshot(AssetValuationType.REAL_ESTATE, asset.id, cents, currency)
                    if (mouvementCapitalCents != null && mouvementCapitalCents != 0L) {
                        ucEnregistrerMouvementCapital(asset.id, CompteType.REAL_ESTATE, mouvementCapitalCents, currency)
                    }
                    _event.emit(InvestmentsEvent.Saved)
                }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun updateScpi(
        scpi: ScpiInvestment,
        label: String,
        sharesStr: String,
        shareValueStr: String,
        contributionStr: String,
        currency: Currency,
        acquisitionValueStr: String = "",
        acquisitionDate: LocalDate? = null
    ) {
        val shares = sharesStr.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Nombre de parts invalide")) }
            return
        }
        val shareValue   = shareValueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
        val contribution = contributionStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
        val acquisitionCents = acquisitionValueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() }
        val scpiToUpdate = scpi.copy(label = label, sharesCount = shares, shareValueCents = shareValue,
            monthlyContributionCents = contribution, currency = currency, updatedAt = LocalDate.now(),
            acquisitionValueCents = acquisitionCents, acquisitionDate = acquisitionDate)
        viewModelScope.launch {
            ucUpdateScpi(scpiToUpdate)
                .onSuccess {
                    ucSaveAssetSnapshot(AssetValuationType.SCPI, scpi.id, scpiToUpdate.totalValueCents, currency)
                    _event.emit(InvestmentsEvent.Saved)
                }
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

    fun addCustomAsset(label: String, valueStr: String, currency: Currency, acquisitionValueStr: String = "", acquisitionDate: LocalDate? = null) {
        val cents = valueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Montant invalide")) }
            return
        }
        val acquisitionCents = acquisitionValueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() }
        viewModelScope.launch {
            ucSaveCustomAsset(CustomAsset(label = label, totalValueCents = cents, currency = currency, updatedAt = LocalDate.now(),
                acquisitionValueCents = acquisitionCents, acquisitionDate = acquisitionDate))
                .onSuccess { newId ->
                    ucSaveAssetSnapshot(AssetValuationType.CUSTOM_ASSET, newId, cents, currency)
                    _event.emit(InvestmentsEvent.Saved)
                }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun updateCustomAsset(asset: CustomAsset, label: String, valueStr: String, currency: Currency, acquisitionValueStr: String = "", acquisitionDate: LocalDate? = null, mouvementCapitalCents: Long? = null) {
        val cents = valueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Montant invalide")) }
            return
        }
        val acquisitionCents = acquisitionValueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() }
        viewModelScope.launch {
            ucUpdateCustomAsset(asset.copy(label = label, totalValueCents = cents, currency = currency, updatedAt = LocalDate.now(),
                acquisitionValueCents = acquisitionCents, acquisitionDate = acquisitionDate))
                .onSuccess {
                    ucSaveAssetSnapshot(AssetValuationType.CUSTOM_ASSET, asset.id, cents, currency)
                    if (mouvementCapitalCents != null && mouvementCapitalCents != 0L) {
                        ucEnregistrerMouvementCapital(asset.id, CompteType.CUSTOM_ASSET, mouvementCapitalCents, currency)
                    }
                    _event.emit(InvestmentsEvent.Saved)
                }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun deleteCustomAsset(asset: CustomAsset) {
        viewModelScope.launch { ucDeleteCustomAsset(asset) }
    }

    // ─── Épargne salariale ─────────────────────────────────────────────────────

    fun addEmployeeSavings(
        type: EmployeeSavingsType,
        label: String,
        balanceStr: String,
        contributionStr: String,
        currency: Currency,
        acquisitionValueStr: String = "",
        acquisitionDate: LocalDate? = null
    ) {
        val balance = balanceStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Solde invalide")) }
            return
        }
        val contribution = contributionStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
        val acquisitionCents = acquisitionValueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() }
        viewModelScope.launch {
            ucSaveEmployeeSavings(EmployeeSavings(type = type, label = label, currentBalanceCents = balance, employerContributionCents = contribution, currency = currency, updatedAt = LocalDate.now(),
                acquisitionValueCents = acquisitionCents, acquisitionDate = acquisitionDate))
                .onSuccess { newId ->
                    ucSaveAssetSnapshot(AssetValuationType.EMPLOYEE_SAVINGS, newId, balance, currency)
                    _event.emit(InvestmentsEvent.Saved)
                }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun updateEmployeeSavings(
        savings: EmployeeSavings,
        type: EmployeeSavingsType,
        label: String,
        balanceStr: String,
        contributionStr: String,
        currency: Currency,
        acquisitionValueStr: String = "",
        acquisitionDate: LocalDate? = null
    ) {
        val balance = balanceStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Solde invalide")) }
            return
        }
        val contribution = contributionStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
        val acquisitionCents = acquisitionValueStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() }
        viewModelScope.launch {
            ucUpdateEmployeeSavings(savings.copy(type = type, label = label, currentBalanceCents = balance, employerContributionCents = contribution, currency = currency, updatedAt = LocalDate.now(),
                acquisitionValueCents = acquisitionCents, acquisitionDate = acquisitionDate))
                .onSuccess {
                    ucSaveAssetSnapshot(AssetValuationType.EMPLOYEE_SAVINGS, savings.id, balance, currency)
                    _event.emit(InvestmentsEvent.Saved)
                }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun deleteEmployeeSavings(savings: EmployeeSavings) {
        viewModelScope.launch { ucDeleteEmployeeSavings(savings) }
    }

    /**
     * Applique l'abondement du mois sur un plan d'épargne salariale : enregistre le versement
     * (non-rétroactif, un par mois) et ajoute son montant au solde. Demande explicite de
     * Florent (2026-08-21) après confirmation sur device que le solde ne bougeait pas -
     * contrairement au choix initial, assumé en connaissance de cause : si le solde réel est
     * aussi remis à jour manuellement depuis le relevé AXA (le fonds investi fluctue avec le
     * marché, pas juste l'abondement), l'abondement sera compté deux fois. À Florent de ne
     * plus resaisir manuellement l'abondement du mois lors de la prochaine mise à jour du solde.
     */
    fun appliquerVersementEmployeeSavings(savings: EmployeeSavings) {
        val now = LocalDate.now()
        viewModelScope.launch {
            if (ucExisteVersementMois(savings.id, CompteType.EMPLOYEE_SAVINGS, now.year, now.monthValue)) {
                _event.emit(InvestmentsEvent.Error("Versement déjà enregistré pour ce mois"))
                return@launch
            }
            val versement = MonthlyVersement(
                accountId    = savings.id,
                compteType   = CompteType.EMPLOYEE_SAVINGS,
                year         = now.year,
                month        = now.monthValue,
                montantCents = savings.employerContributionCents,
                currency     = savings.currency
            )
            ucSaveVersement(versement)
                .onSuccess {
                    ucUpdateEmployeeSavings(
                        savings.copy(
                            currentBalanceCents = savings.currentBalanceCents + savings.employerContributionCents,
                            updatedAt = now
                        )
                    )
                    _event.emit(InvestmentsEvent.VersementApplique)
                }
                .onFailure { _event.emit(InvestmentsEvent.Error("Vérifier les informations saisies")) }
        }
    }

    /**
     * Applique le versement mensuel prévu sur une SCPI : enregistre le versement (non-rétroactif,
     * un par mois) et ajoute sa contre-valeur au nombre de parts, au cours actuel de la part.
     * Même demande et même risque de double-compte que [appliquerVersementEmployeeSavings]
     * (voir sa doc) si le nombre de parts réel est aussi resaisi manuellement après relevé.
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
                    val partsAchetees = scpi.monthlyContributionCents.toDouble() / scpi.shareValueCents
                    ucUpdateScpi(
                        scpi.copy(
                            sharesCount = scpi.sharesCount + partsAchetees,
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
        val patrimoineTrendPct    : Float?                   = null,
        val hasConvertedValues    : Boolean                  = false
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
