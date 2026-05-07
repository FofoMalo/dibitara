package com.dibitara.app.presentation.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.AirbnbRental
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.RealEstateAsset
import com.dibitara.app.domain.model.ScpiInvestment
import com.dibitara.app.domain.usecase.DeleteInvestmentUseCase
import com.dibitara.app.domain.usecase.GetInvestmentsUseCase
import com.dibitara.app.domain.usecase.SaveInvestmentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class InvestmentsViewModel @Inject constructor(
    private val getInvestments: GetInvestmentsUseCase,
    private val saveInvestment: SaveInvestmentUseCase,
    private val deleteInvestment: DeleteInvestmentUseCase
) : ViewModel() {

    private val currentYear = LocalDate.now().year

    val uiState: StateFlow<InvestmentsUiState> = combine(
        getInvestments.realEstate(),
        getInvestments.scpi(),
        getInvestments.airbnbByYear(currentYear)
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
            saveInvestment.realEstate(
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
            saveInvestment.scpi(
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
            saveInvestment.airbnb(
                AirbnbRental(propertyLabel = label, amountCents = cents, date = date, currency = currency)
            )
                .onSuccess { _event.emit(InvestmentsEvent.Saved) }
                .onFailure { _event.emit(InvestmentsEvent.Error(it.message ?: "Erreur")) }
        }
    }

    fun deleteRealEstate(asset: RealEstateAsset) {
        viewModelScope.launch { deleteInvestment.realEstate(asset) }
    }

    fun deleteScpi(scpi: ScpiInvestment) {
        viewModelScope.launch { deleteInvestment.scpi(scpi) }
    }

    fun deleteAirbnb(rental: AirbnbRental) {
        viewModelScope.launch { deleteInvestment.airbnb(rental) }
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
    data class Error(val message: String) : InvestmentsEvent()
}
