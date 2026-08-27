package com.dibitara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id                  : Long = 0,
    val provider            : String,
    val label               : String,
    val currentBalanceCents : Long,
    val currency            : String,
    val updatedAtEpochDay   : Long
)
