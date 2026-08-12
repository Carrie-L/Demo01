package com.carrie.demo01

import android.content.Context
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.TextView
import kotlin.math.roundToInt

/**
 * Keeps the storage unit at 10sp for normal/large content scales. The unit
 * follows the content scale only below 1x, while the numeric part can shrink
 * further when the complete value would otherwise exceed the assigned width.
 */
class CapacityTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
) : TextView(context, attrs, defStyleAttr) {

    private var number = "0.0"
    private var unit = "KB"
    private var contentScale = 1f

    init {
        setSingleLine(true)
        // Never replace the capacity with "...". The number is fitted below.
        ellipsize = null
        textDirection = TEXT_DIRECTION_LTR
    }

    fun setCapacity(bytes: Long, scale: Float) {
        val formatted = StorageSizeFormatter.format(bytes)
        number = formatted.number
        unit = formatted.unit
        contentScale = scale.coerceIn(MIN_CONTENT_SCALE, MAX_CONTENT_SCALE)
        contentDescription = "$number $unit"
        rebuildText()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width != oldWidth) rebuildText()
    }

    private fun rebuildText() {
        val availableWidth = width - totalPaddingLeft - totalPaddingRight

        // Always derive the unit size from the immutable 10sp resource. This
        // prevents a previously rendered large size from leaking into a later
        // bind or a recreated Activity.
        val unitScale = contentScale.coerceAtMost(1f)
        val unitTextSizePx = (
            resources.getDimension(R.dimen.cs_10_sp) * unitScale
        ).roundToInt().coerceAtLeast(1)

        val requestedNumberSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            BASE_NUMBER_TEXT_SIZE_SP * contentScale,
            resources.displayMetrics,
        ).roundToInt().coerceAtLeast(1)

        val fittedNumberSizePx = if (availableWidth <= 0) {
            requestedNumberSizePx
        } else {
            findLargestFittingNumberSize(
                requestedNumberSizePx = requestedNumberSizePx,
                unitTextSizePx = unitTextSizePx,
                widthLimitPx = (availableWidth - FIT_SAFETY_PX).coerceAtLeast(1),
            )
        }

        text = buildStyledText(fittedNumberSizePx, unitTextSizePx)
    }

    /**
     * Measure the final styled string, not the number and unit separately.
     * This includes span processing, boundary kerning and TextView paint
     * properties, which prevents a near-limit value from becoming ellipsized.
     */
    private fun findLargestFittingNumberSize(
        requestedNumberSizePx: Int,
        unitTextSizePx: Int,
        widthLimitPx: Int,
    ): Int {
        if (
            measureStyledText(requestedNumberSizePx, unitTextSizePx) <=
            widthLimitPx.toFloat()
        ) {
            return requestedNumberSizePx
        }

        var low = 1
        var high = requestedNumberSizePx
        var best = 1
        while (low <= high) {
            val candidate = (low + high) ushr 1
            if (measureStyledText(candidate, unitTextSizePx) <= widthLimitPx.toFloat()) {
                best = candidate
                low = candidate + 1
            } else {
                high = candidate - 1
            }
        }
        return best
    }

    private fun measureStyledText(numberTextSizePx: Int, unitTextSizePx: Int): Float {
        val styledText = buildStyledText(numberTextSizePx, unitTextSizePx)
        return Layout.getDesiredWidth(styledText, TextPaint(paint))
    }

    private fun buildStyledText(
        numberTextSizePx: Int,
        unitTextSizePx: Int,
    ): SpannableString {
        val fullText = number + unit
        return SpannableString(fullText).apply {
            setSpan(
                AbsoluteSizeSpan(numberTextSizePx),
                0,
                number.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            if (unit.isNotEmpty()) {
                setSpan(
                    AbsoluteSizeSpan(unitTextSizePx),
                    number.length,
                    fullText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
    }

    companion object {
        private const val BASE_NUMBER_TEXT_SIZE_SP = 18f
        private const val MIN_CONTENT_SCALE = 0.85f
        private const val MAX_CONTENT_SCALE = 1.45f
        private const val FIT_SAFETY_PX = 2
    }
}
