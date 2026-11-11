package com.piashcse.constants

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoyaltyEnumsTest {
    @Test
    fun `loyalty enums expose their lifecycle values`() {
        assertEquals(8, WalletEntryType.entries.size)
        assertEquals(3, ReferralStatus.entries.size)
        assertEquals(4, GiftCardStatus.entries.size)
        assertTrue(WalletEntryType.POINTS_REDEEM in WalletEntryType.entries)
        assertTrue(ReferralStatus.COMPLETED in ReferralStatus.entries)
        assertTrue(GiftCardStatus.VOID in GiftCardStatus.entries)
    }
}
