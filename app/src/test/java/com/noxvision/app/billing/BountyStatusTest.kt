package com.noxvision.app.billing

import org.junit.Assert.assertEquals
import org.junit.Test

class BountyStatusTest {

    @Test
    fun testBountyStatusEntries() {
        val expected = listOf(BountyStatus.ACTIVE, BountyStatus.IN_DEV, BountyStatus.SHIPPED)
        assertEquals(expected, BountyStatus.entries)
    }
}
