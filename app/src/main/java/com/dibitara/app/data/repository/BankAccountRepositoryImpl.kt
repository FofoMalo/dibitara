package com.dibitara.app.data.repository

import com.dibitara.app.data.local.dao.BankAccountDao
import com.dibitara.app.data.local.entity.BankAccountEntity
import com.dibitara.app.domain.model.BankAccount
import com.dibitara.app.domain.model.BankProvider
import com.dibitara.app.domain.repository.BankAccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BankAccountRepositoryImpl @Inject constructor(
    private val dao: BankAccountDao
) : BankAccountRepository {

    override fun getAll(): Flow<List<BankAccount>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun findByProvider(provider: BankProvider): BankAccount? =
        dao.findByProvider(provider.name)?.toDomain()

    override suspend fun upsert(account: BankAccount): Long =
        dao.upsert(account.toEntity())

    override suspend fun delete(account: BankAccount) =
        dao.delete(account.toEntity())

    // ─── Mapping entité ↔ domaine ─────────────────────────────────────────────
    // La conversion vit sur BankAccountEntity (réutilisée par la restauration JSON).

    private fun BankAccount.toEntity() = BankAccountEntity.fromDomain(this)
}
