package com.carrie.demo01

import java.util.Locale

data class FormattedStorageSize(
    val number: String,
    val unit: String,
)

object StorageSizeFormatter {
    private const val BYTES_PER_KB = 1024.0
    private const val BYTES_PER_MB = BYTES_PER_KB * 1024.0
    private const val BYTES_PER_G = BYTES_PER_MB * 1024.0

    fun format(bytes: Long): FormattedStorageSize {
        val safeBytes = bytes.coerceAtLeast(0).toDouble()
        val (value, unit) = when {
            safeBytes >= BYTES_PER_G -> safeBytes / BYTES_PER_G to "G"
            safeBytes >= BYTES_PER_MB -> safeBytes / BYTES_PER_MB to "MB"
            else -> safeBytes / BYTES_PER_KB to "KB"
        }
        return FormattedStorageSize(
            number = String.format(Locale.US, "%.1f", value),
            unit = unit,
        )
    }
}
