package com.dibitara.app.presentation.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.usecase.DeleteAirbnbRentalUseCase
import com.dibitara.app.domain.usecase.DeleteRealEstateUseCase
import com.dibitara.app.domain.usecase.DeleteScpiUseCase
import com.dibitara.app.domain.usecase.ExisteVersementMoisUseCase
import com.dibitara.app.domain.usecase.GetAirbnbRentalsByYearUseCase
import com.dibitara.app.domain.usecase.GetRealEstateUseCase
import com.dibitara.app.domain.usecase.GetScpiUseCase
import com.dibitara.app.domain.usecase.SaveAirbnbRentalUseCase
import com.dibitara.app.domain.usecase.SaveRealEstateUseCase
import com.dibitara.app.domain.usecase.SaveScpiUseCase
import com.dibitara.app.domain.usecase.SaveVersementUseCase
import com.dibitara.app.domain.usecase.UpdateAirbnbRentalUseCase
import com.dibitara.app.domain.usecase.UpdateRealEstateUseCase
import com.dibitara.app.domain.usecase.UpdateScpiUseCase
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
    private val ucSaveRealEstate: SaveRealEstateUseCase,
    private val ucSaveScpi: SaveScpiUseCase,
    private val ucSaveAirbnbRental: SaveAirbnbRentalUseCase,
    private val ucUpdateRealEstate: UpdateRealEstateUseCase,
    private val ucUpdateScpi: UpdateScpiUseCase,
    private val ucUpdateAirbnbRental: UpdateAirbnbRentalUseCase,
    private val ucDeleteRealEstate: DeleteRealEstateUseCase,
    private val ucDeleteScpi: DeleteScpiUseCase,
    private val ucDeleteAirbnbRental: DeleteAirbnbRentalUseCase,
    private val ucSaveVersement: SaveVersementUseCase,
    private val ucExisteVersementMois: ExisteVersementMoisUseCase
) : ViewModel() {

    val uiState: StateFlow<InvestmentsUiState> = combine(
        ucGetRealEstate(),
        ucGetScpi(),
        ucGetAirbnbByYear(LocalDate.now().year)
    ) { realEstate, scpi, airbnb ->
        InvestmentsUiState.Success(
            realEstate = realEstate,
            scpi = scpi,
            airbnbRentals = airbnb,
            airbnbAnnualTotal = airbnb.sumOf { it.amountCents }
        ) as InvestmentsUiState
    }
        .catch { emit(InvestmentsUiState.Error(it.message ?: "Erreur inconnue")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InvestmentsUiState.Loading
        )

    private val _event = MutableSharedFlow<InvestmentsEvent>()
    val event: SharedFlow<InvestmentsEvent> = _event.asSharedFlow()

    fun addRealEstate(label: String, valueStr: String, currency: Currency) {
        val cents = valueStr.toDoubleOrNull()?.let { (it * 100).toLong() } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Montant invalide")) }
            return
        }
        viewModelScope.launch {
            ucSaveRealEstate(
                RealEstateAsset(label = label, currentValueCents = cents, currency = currency, updatedAt = LocalDate.now())
            )
                .onSuccess { _event.emit(InvestmentsEvent.Saved) }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun addScpi(label: String, sharesStr: String, shareValueStr: String, contributionStr: String, currency: Currency) {
        val shares = sharesStr.toIntOrNull() ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Nombre de parts invalide")) }
            return
        }
        val shareValue = shareValueStr.toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
        val contribution = contributionStr.toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
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
        val cents = amountStr.toDoubleOrNull()?.let { (it * 100).toLong() } ?: run {
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

    fun updateRealEstate(asset: RealEstateAsset, label: String, valueStr: String, currency: Currency) {
        val cents = valueStr.toDoubleOrNull()?.let { (it * 100).toLong() } ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Montant invalide")) }
            return
        }
        viewModelScope.launch {
            ucUpdateRealEstate(asset.copy(label = label, currentValueCents = cents, currency = currency, updatedAt = LocalDate.now()))
                .onSuccess { _event.emit(InvestmentsEvent.Saved) }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun updateScpi(scpi: ScpiInvestment, label: String, sharesStr: String, shareValueStr: String, contributionStr: String, currency: Currency) {
        val shares = sharesStr.toIntOrNull() ?: run {
            viewModelScope.launch { _event.emit(InvestmentsEvent.Error("Nombre de parts invalide")) }
            return
        }
        val shareValue   = shareValueStr.toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
        val contribution = contributionStr.toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
        viewModelScope.launch {
            ucUpdateScpi(scpi.copy(label = label, sharesCount = shares, shareValueCents = shareValue, monthlyContributionCents = contribution, currency = currency, updatedAt = LocalDate.now()))
                .onSuccess { _event.emit(InvestmentsEvent.Saved) }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun updateAirbnbRental(rental: AirbnbRental, label: String, amountStr: String, currency: Currency) {
        val cents = amountStr.toDoubleOrNull()?.let { (it * 100).toLong() } ?: run {
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
        val realEstate: List<RealEstateAsset>,
        val scpi: List<ScpiInvestment>,
        val airbnbRentals: List<AirbnbRental>,
        val airbnbAnnualTotal: Long
    ) : InvestmentsUiState()
    data class Error(val message: String) : InvestmentsUiState()
}

sealed class InvestmentsEvent {
    data object Saved : InvestmentsEvent()
    data object VersementApplique : InvestmentsEvent()
    data class Error(val message: String) : InvestmentsEvent()
}
