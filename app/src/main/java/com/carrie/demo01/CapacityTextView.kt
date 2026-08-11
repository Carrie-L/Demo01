package com.carrie.demo01

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.TextView
import kotlin.math.max
import kotlin.math.min
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
        val fullText = number + unit
        val availableWidth = width - totalPaddingLeft - totalPaddingRight

        // Always derive the unit size from the immutable 10sp resource. This
        // prevents a previously rendered large size from leaking into a later
        // bind or a recreated Activity.
        val unitScale = contentScale.coerceAtMost(1f)
        val unitTextSizePx = (
            resources.getDimension(R.dimen.cs_10_sp) * unitScale
        ).roundToInt().coerceAtLeast(1)
        val unitPaint = android.text.TextPaint(paint).apply {
            textSize = unitTextSizePx.toFloat()
        }
        val unitWidth = unitPaint.measureText(unit)

        val requestedNumberSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            BASE_NUMBER_TEXT_SIZE_SP * contentScale,
            resources.displayMetrics,
        )
        val numberPaint = android.text.TextPaint(paint).apply {
            textSize = requestedNumberSizePx
        }
        val requestedNumberWidth = numberPaint.measureText(number)
        val fittedNumberSizePx = if (availableWidth <= 0) {
            requestedNumberSizePx
        } else {
            val numberWidthLimit = max(1f, availableWidth - unitWidth)
            val fitRatio = if (requestedNumberWidth == 0f) {
                1f
            } else {
                min(1f, numberWidthLimit / requestedNumberWidth)
            }
            max(1f, requestedNumberSizePx * fitRatio)
        }

        text = SpannableString(fullText).apply {
            setSpan(
                // Floor instead of rounding up so the measured text never
                // exceeds its allotted width because of pixel rounding.
                AbsoluteSizeSpan(fittedNumberSizePx.toInt().coerceAtLeast(1)),
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
    }
}
