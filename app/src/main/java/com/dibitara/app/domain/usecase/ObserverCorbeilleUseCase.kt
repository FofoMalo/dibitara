package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.repository.TransactionActionsRepository
import javax.inject.Inject

class ObserverCorbeilleUseCase @Inject constructor(private val repository: TransactionActionsRepository) {
    operator fun invoke() = repository.corbeille()
}
