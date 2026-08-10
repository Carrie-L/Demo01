package com.carrie.demo01

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageSizeFormatterTest {

    @Test
    fun formatsMockValueAsMegabytes() {
        assertEquals(
            FormattedStorageSize("322.5", "MB"),
            StorageSizeFormatter.format((322.5 * 1024 * 1024).toLong()),
        )
    }

    @Test
    fun switchesUnitsAtBinaryBoundaries() {
        assertEquals(
            FormattedStorageSize("1023.0", "KB"),
            StorageSizeFormatter.format(1023L * 1024),
        )
        assertEquals(
            FormattedStorageSize("1.0", "MB"),
            StorageSizeFormatter.format(1024L * 1024),
        )
        assertEquals(
            FormattedStorageSize("1.0", "G"),
            StorageSizeFormatter.format(1024L * 1024 * 1024),
        )
    }

    @Test
    fun clampsNegativeValuesToZeroKilobytes() {
        assertEquals(
            FormattedStorageSize("0.0", "KB"),
            StorageSizeFormatter.format(-1),
        )
    }
}
