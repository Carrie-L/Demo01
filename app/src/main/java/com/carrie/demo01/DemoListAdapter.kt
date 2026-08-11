package com.carrie.demo01

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

private const val BASE_VALUE_TEXT_SIZE_SP = 18f
private const val BASE_LABEL_TEXT_SIZE_SP = 14f

data class StatItem(
    val value: String? = null,
    val storageBytes: Long? = null,
    @StringRes val labelRes: Int,
)

class DemoListAdapter(initialScale: Float) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var currentScale: Float = initialScale.coerceIn(MIN_SCALE, MAX_SCALE)
        private set

    private val statItems = listOf(
        StatItem(value = "0", labelRes = R.string.follow),
        StatItem(value = "1", labelRes = R.string.download),
        StatItem(value = "3", labelRes = R.string.bookmark),
        StatItem(value = "2", labelRes = R.string.favorite),
        StatItem(
            storageBytes = (322.5 * 1024 * 1024).toLong(),
            labelRes = R.string.storage_cleanup,
        ),
    )

    override fun getItemCount(): Int = MOCK_CONTENT_COUNT + FIXED_ITEM_COUNT

    override fun getItemViewType(position: Int): Int = when (position) {
        SCALE_CONTROL_POSITION -> VIEW_TYPE_SCALE_CONTROL
        STAT_CARD_POSITION -> VIEW_TYPE_STAT_CARD
        else -> VIEW_TYPE_MOCK_CONTENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SCALE_CONTROL -> ScaleControlViewHolder(
                inflater.inflate(R.layout.item_font_scale_control, parent, false),
            )
            VIEW_TYPE_STAT_CARD -> StatCardViewHolder(
                inflater.inflate(R.layout.item_stat_card, parent, false),
                statItems,
            )
            else -> MockContentViewHolder(
                inflater.inflate(R.layout.item_mock_content, parent, false),
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ScaleControlViewHolder -> holder.bind(currentScale, ::updateScale)
            is StatCardViewHolder -> holder.bind(currentScale)
            is MockContentViewHolder -> holder.bind(position - FIXED_ITEM_COUNT + 1)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        if (holder is StatCardViewHolder && payloads.contains(PAYLOAD_SCALE)) {
            holder.bind(currentScale)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private fun updateScale(scale: Float) {
        val newScale = scale.coerceIn(MIN_SCALE, MAX_SCALE)
        if (newScale == currentScale) return
        currentScale = newScale
        notifyItemChanged(STAT_CARD_POSITION, PAYLOAD_SCALE)
    }

    private class ScaleControlViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val scaleValue = itemView.findViewById<TextView>(R.id.font_scale_value)
        private val seekBar = itemView.findViewById<SeekBar>(R.id.font_scale_seek_bar)

        fun bind(scale: Float, onScaleChanged: (Float) -> Unit) {
            seekBar.setOnSeekBarChangeListener(null)
            seekBar.progress = (
                (scale - MIN_SCALE) / (MAX_SCALE - MIN_SCALE) * seekBar.max
            ).roundToInt()
            updateScaleText(scale)
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    val maxProgress = seekBar?.max?.coerceAtLeast(1) ?: 100
                    val fraction = progress / maxProgress.toFloat()
                    val newScale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * fraction
                    updateScaleText(newScale)
                    onScaleChanged(newScale)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }

        private fun updateScaleText(scale: Float) {
            scaleValue.text = itemView.context.getString(R.string.font_scale_value, scale)
        }
    }

    private class StatCardViewHolder(
        itemView: View,
        private val items: List<StatItem>,
    ) : RecyclerView.ViewHolder(itemView) {
        private val recyclerView = itemView as RecyclerView
        private val spacingPx = itemView.resources.getDimensionPixelSize(R.dimen.stat_item_spacing)
        private val valueLabelSpacingPx =
            itemView.resources.getDimensionPixelSize(R.dimen.stat_value_label_spacing)
        private val statAdapter = StatAdapter(items)
        private var currentScale = 1f

        init {
            recyclerView.layoutManager = NonScrollableHorizontalLayoutManager(itemView.context)
            recyclerView.adapter = statAdapter
            recyclerView.itemAnimator = null
            recyclerView.addItemDecoration(HorizontalSpacingDecoration(spacingPx))
            recyclerView.addOnLayoutChangeListener { view, left, _, right, _, oldLeft, _, oldRight, _ ->
                val width = right - left
                val oldWidth = oldRight - oldLeft
                if (width != oldWidth) updateItemLayout(view.width)
            }
            recyclerView.post { updateItemLayout(recyclerView.width) }
        }

        fun bind(scale: Float) {
            currentScale = scale.coerceIn(MIN_SCALE, MAX_SCALE)
            updateItemLayout(recyclerView.width)
            if (recyclerView.width <= 0) {
                recyclerView.post { updateItemLayout(recyclerView.width) }
            }
        }

        private fun updateItemLayout(recyclerViewWidth: Int) {
            if (recyclerViewWidth <= 0 || statAdapter.itemCount == 0) return
            val availableWidth = recyclerViewWidth - recyclerView.paddingLeft - recyclerView.paddingRight
            val totalSpacing = spacingPx * (statAdapter.itemCount - 1)
            val itemWidth = ((availableWidth - totalSpacing) / statAdapter.itemCount).coerceAtLeast(1)
            val commonLabelTextSizePx = calculateCommonLabelTextSize(itemWidth)
            val itemHeight = calculateItemHeight(commonLabelTextSizePx)
            if (recyclerView.isComputingLayout) {
                recyclerView.post {
                    applyItemLayout(itemWidth, itemHeight, commonLabelTextSizePx)
                }
            } else {
                applyItemLayout(itemWidth, itemHeight, commonLabelTextSizePx)
            }
        }

        private fun applyItemLayout(
            itemWidth: Int,
            itemHeight: Int,
            commonLabelTextSizePx: Float,
        ) {
            val recyclerViewHeight =
                recyclerView.paddingTop + itemHeight + recyclerView.paddingBottom
            if (recyclerView.layoutParams.height != recyclerViewHeight) {
                recyclerView.layoutParams = recyclerView.layoutParams.apply {
                    height = recyclerViewHeight
                }
            }
            statAdapter.updateLayout(
                newScale = currentScale,
                newItemWidth = itemWidth,
                newLabelTextSizePx = commonLabelTextSizePx,
            )
        }

        /**
         * Calculate one shared size from the widest of all five labels. If the
         * widest label needs to shrink, every label receives the same result.
         */
        private fun calculateCommonLabelTextSize(itemWidth: Int): Float {
            val requestedSizePx = spToPx(BASE_LABEL_TEXT_SIZE_SP * currentScale)
            val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = requestedSizePx
                typeface = Typeface.create("sans", Typeface.NORMAL)
            }
            val widestLabelWidth = items.maxOf { item ->
                labelPaint.measureText(itemView.context.getString(item.labelRes))
            }
            val widthLimit = (itemWidth - 1).coerceAtLeast(1).toFloat()

            return if (widestLabelWidth > widthLimit) {
                (requestedSizePx * widthLimit / widestLabelWidth).coerceAtLeast(1f)
            } else {
                requestedSizePx
            }
        }

        /** Give every child the same exact height and therefore the same bottom edge. */
        private fun calculateItemHeight(commonLabelTextSizePx: Float): Int {
            val numberPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = spToPx(BASE_VALUE_TEXT_SIZE_SP * currentScale)
                typeface = Typeface.create("sans", Typeface.BOLD)
            }
            val numberMetrics = numberPaint.fontMetricsInt
            val numberHeight = numberMetrics.descent - numberMetrics.ascent

            val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = commonLabelTextSizePx
                typeface = Typeface.create("sans", Typeface.NORMAL)
            }
            val labelMetrics = labelPaint.fontMetricsInt
            val labelHeight = labelMetrics.descent - labelMetrics.ascent

            return numberHeight + valueLabelSpacingPx + labelHeight
        }

        private fun spToPx(value: Float): Float = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            value,
            itemView.resources.displayMetrics,
        )
    }

    private class MockContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title = itemView.findViewById<TextView>(R.id.mock_title)

        fun bind(index: Int) {
            title.text = itemView.context.getString(R.string.mock_item_title, index)
        }
    }

    companion object {
        private const val VIEW_TYPE_SCALE_CONTROL = 0
        private const val VIEW_TYPE_STAT_CARD = 1
        private const val VIEW_TYPE_MOCK_CONTENT = 2
        private const val SCALE_CONTROL_POSITION = 0
        private const val STAT_CARD_POSITION = 1
        private const val FIXED_ITEM_COUNT = 2
        private const val MOCK_CONTENT_COUNT = 6
        private const val MIN_SCALE = 0.85f
        private const val MAX_SCALE = 1.45f
        private const val PAYLOAD_SCALE = "payload_scale"
    }
}

private class StatAdapter(
    private val items: List<StatItem>,
) : RecyclerView.Adapter<StatAdapter.StatViewHolder>() {

    private var scale = 1f
    private var itemWidth = ViewGroup.LayoutParams.WRAP_CONTENT
    private var labelTextSizePx = 0f

    override fun getItemViewType(position: Int): Int =
        if (items[position].storageBytes == null) {
            VIEW_TYPE_VALUE
        } else {
            VIEW_TYPE_CAPACITY
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatViewHolder {
        val layoutRes = if (viewType == VIEW_TYPE_CAPACITY) {
            R.layout.item_capacity_stat
        } else {
            R.layout.item_stat
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        if (parent.measuredWidth > 0) {
            val spacing = parent.resources.getDimensionPixelSize(R.dimen.stat_item_spacing)
            val availableWidth = parent.measuredWidth - parent.paddingLeft - parent.paddingRight
            itemWidth = ((availableWidth - spacing * (items.size - 1)) / items.size).coerceAtLeast(1)
        }
        view.layoutParams.width = itemWidth
        return if (viewType == VIEW_TYPE_CAPACITY) {
            CapacityStatViewHolder(view)
        } else {
            ValueStatViewHolder(view)
        }
    }

    override fun getItemCount(): Int = items.size.coerceAtMost(MAX_ITEMS)

    override fun onBindViewHolder(holder: StatViewHolder, position: Int) {
        holder.itemView.layoutParams = holder.itemView.layoutParams.apply { width = itemWidth }
        holder.bind(items[position], scale, labelTextSizePx)
    }

    fun updateLayout(
        newScale: Float,
        newItemWidth: Int,
        newLabelTextSizePx: Float,
    ) {
        if (newItemWidth <= 0 || newLabelTextSizePx <= 0f) return
        if (
            newScale == scale &&
            newItemWidth == itemWidth &&
            newLabelTextSizePx == labelTextSizePx
        ) {
            return
        }
        scale = newScale
        itemWidth = newItemWidth
        labelTextSizePx = newLabelTextSizePx
        notifyItemRangeChanged(0, itemCount)
    }

    abstract class StatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val label = itemView.findViewById<TextView>(R.id.stat_label)

        fun bind(item: StatItem, scale: Float, commonLabelTextSizePx: Float) {
            label.setText(item.labelRes)
            if (commonLabelTextSizePx > 0f) {
                label.setTextSize(TypedValue.COMPLEX_UNIT_PX, commonLabelTextSizePx)
            } else {
                label.setTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    BASE_LABEL_TEXT_SIZE_SP * scale,
                )
            }
            bindValue(item, scale)
        }

        protected abstract fun bindValue(item: StatItem, scale: Float)
    }

    class ValueStatViewHolder(itemView: View) : StatViewHolder(itemView) {
        private val value = itemView.findViewById<TextView>(R.id.stat_value)

        override fun bindValue(item: StatItem, scale: Float) {
            value.text = item.value.orEmpty()
            value.textSize = BASE_VALUE_TEXT_SIZE_SP * scale
        }
    }

    class CapacityStatViewHolder(itemView: View) : StatViewHolder(itemView) {
        private val capacity = itemView.findViewById<CapacityTextView>(R.id.stat_capacity)

        override fun bindValue(item: StatItem, scale: Float) {
            capacity.setCapacity(item.storageBytes ?: 0L, scale)
        }
    }

    companion object {
        private const val VIEW_TYPE_VALUE = 0
        private const val VIEW_TYPE_CAPACITY = 1
        private const val MAX_ITEMS = 5
    }
}

private class NonScrollableHorizontalLayoutManager(context: android.content.Context) :
    LinearLayoutManager(context, HORIZONTAL, false) {
    override fun canScrollHorizontally(): Boolean = false
}

private class HorizontalSpacingDecoration(
    private val spacingPx: Int,
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position != RecyclerView.NO_POSITION && position < state.itemCount - 1) {
            outRect.right = spacingPx
        }
    }
}
