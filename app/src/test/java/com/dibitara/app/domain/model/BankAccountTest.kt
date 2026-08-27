package com.dibitara.app.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BankAccountTest {

    @Test
    fun `fromImportSource reconnaît les sources BRED (csv, pdf, notification)`() {
        assertEquals(BankProvider.BRED, BankProvider.fromImportSource("bred"))
        assertEquals(BankProvider.BRED, BankProvider.fromImportSource("bred_notification"))
    }

    @Test
    fun `fromImportSource reconnaît TradeRepublic`() {
        assertEquals(BankProvider.TRADE_REPUBLIC, BankProvider.fromImportSource("trade_republic"))
    }

    @Test
    fun `fromImportSource retourne null pour une source inconnue ou absente`() {
        assertNull(BankProvider.fromImportSource("autre_source"))
        assertNull(BankProvider.fromImportSource(null))
    }
}
