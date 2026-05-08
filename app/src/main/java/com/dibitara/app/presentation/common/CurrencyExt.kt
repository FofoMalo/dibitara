package com.dibitara.app.presentation.common

import com.dibitara.app.domain.model.Currency

fun Long.toCurrencyDisplay(currency: Currency): String =
    "%.2f %s".format(this / 100.0, currency.symbol)
