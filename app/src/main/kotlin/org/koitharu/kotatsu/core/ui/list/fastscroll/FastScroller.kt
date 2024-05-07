package org.koitharu.kotatsu.core.ui.list.fastscroll

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.annotation.StyleableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.GravityCompat
import androidx.core.view.ancestors
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.isLayoutReversed
import org.koitharu.kotatsu.databinding.FastScrollerBinding
import kotlin.math.roundToInt
import com.google.android.material.R as materialR

private const val SCROLLBAR_HIDE_DELAY = 1000L
private const val TRACK_SNAP_RANGE = 5

@Suppress("MemberVisibilityCanBePrivate", "unused")
class FastScroller @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = R.attr.fastScrollerStyle,
) : LinearLayout(context, attrs, defStyleAttr) {

	enum class BubbleSize(@DrawableRes val drawableId: Int, @DimenRes val textSizeId: Int) {
		NORMAL(R.drawable.fastscroll_bubble, R.dimen.fastscroll_bubble_text_size),
		SMALL(R.drawable.fastscroll_bubble_small, R.dimen.fastscroll_bubble_text_size_small)
	}

	private val binding = FastScrollerBinding.inflate(LayoutInflater.from(context), this)

	private val scrollbarPaddingEnd = context.resources.getDimension(R.dimen.fastscroll_scrollbar_padding_end)

	@ColorInt
	private var bubbleColor = 0

	@ColorInt
	private var handleColor = 0

	private var bubbleHeight = 0
	private var handleHeight = 0
	private var viewHeight = 0
	private var offset = 0
	private var hideScrollbar = true
	private var showBubble = true
	private var showBubbleAlways = false
	private var bubbleSize = BubbleSize.SMALL
	private var bubbleImage: Drawable? = null
	private var handleImage: Drawable? = null
	private var trackImage: Drawable? = null
	private var recyclerView: RecyclerView? = null
	private val scrollbarAnimator = ScrollbarAnimator(binding.scrollbar, scrollbarPaddingEnd)
	private val bubbleAnimator = BubbleAnimator(binding.bubble)

	private var fastScrollListener: FastScrollListener? = null
	private var sectionIndexer: SectionIndexer? = null

	private val scrollbarHider = Runnable {
		hideBubble()
		hideScrollbar()
	}

	private val scrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
		override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
			if (!binding.thumb.isSelected && isEnabled) {
				val y = recyclerView.scrollProportion
				setViewPositions(y)

				if (showBubbleAlways) {
					val targetPos = getRecyclerViewTargetPosition(y)
					sectionIndexer?.let { bindBubble(it.getSectionText(recyclerView.context, targetPos)) }
				}
			}
		}

		override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
			super.onScrollStateChanged(recyclerView, newState)

			if (isEnabled) {
				when (newState) {
					RecyclerView.SCROLL_STATE_DRAGGING -> {
						handler.removeCallbacks(scrollbarHider)
						showScrollbar()
						if (showBubbleAlways && sectionIndexer != null) showBubble()
					}

					RecyclerView.SCROLL_STATE_IDLE -> if (hideScrollbar && !binding.thumb.isSelected) {
						handler.postDelayed(scrollbarHider, SCROLLBAR_HIDE_DELAY)
					}
				}
			}
		}
	}

	private val RecyclerView.scrollProportion: Float
		get() {
			val rangeDiff = computeVerticalScrollRange() - computeVerticalScrollExtent()
			val proportion = computeVerticalScrollOffset() / if (rangeDiff > 0) rangeDiff.toFloat() else 1f
			return viewHeight * proportion
		}

	val isScrollbarVisible: Boolean
		get() = binding.scrollbar.isVisible

	init {
		clipChildren = false
		orientation = HORIZONTAL

		@ColorInt var bubbleColor = context.getThemeColor(materialR.attr.colorControlNormal, Color.DKGRAY)
		@ColorInt var handleColor = bubbleColor
		@ColorInt var trackColor = context.getThemeColor(materialR.attr.colorOutline, Color.LTGRAY)
		@ColorInt var textColor = context.getThemeColor(android.R.attr.textColorPrimaryInverse, Color.WHITE)

		var showTrack = false

		context.withStyledAttributes(attrs, R.styleable.FastScrollRecyclerView, defStyleAttr) {
			bubbleColor = getColor(R.styleable.FastScrollRecyclerView_bubbleColor, bubbleColor)
			handleColor = getColor(R.styleable.FastScrollRecyclerView_thumbColor, handleColor)
			trackColor = getColor(R.styleable.FastScrollRecyclerView_trackColor, trackColor)
			textColor = getColor(R.styleable.FastScrollRecyclerView_bubbleTextColor, textColor)
			hideScrollbar = getBoolean(R.styleable.FastScrollRecyclerView_hideScrollbar, hideScrollbar)
			showBubble = getBoolean(R.styleable.FastScrollRecyclerView_showBubble, showBubble)
			showBubbleAlways = getBoolean(R.styleable.FastScrollRecyclerView_showBubbleAlways, showBubbleAlways)
			showTrack = getBoolean(R.styleable.FastScrollRecyclerView_showTrack, showTrack)
			bubbleSize = getBubbleSize(R.styleable.FastScrollRecyclerView_bubbleSize, bubbleSize)
			val textSize = getDimension(R.styleable.FastScrollRecyclerView_bubbleTextSize, bubbleSize.textSize)
			binding.bubble.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
			offset = getDimensionPixelOffset(R.styleable.FastScrollRecyclerView_scrollerOffset, offset)
		}

		setTrackColor(trackColor)
		setHandleColor(handleColor)
		setBubbleColor(bubbleColor)
		setBubbleTextColor(textColor)
		setHideScrollbar(hideScrollbar)
		setBubbleVisible(showBubble, showBubbleAlways)
		setTrackVisible(showTrack)
	}

	override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
		super.onSizeChanged(w, h, oldW, oldH)
		viewHeight = h
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(event: MotionEvent): Boolean {
		val setYPositions: () -> Unit = {
			val y = event.y
			setViewPositions(y)
			setRecyclerViewPosition(y)
		}

		when (event.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				if (!isScrollbarVisible || event.x.toInt() !in binding.scrollbar.left..binding.scrollbar.right) {
					return false
				}

				requestDisallowInterceptTouchEvent(true)
				setHandleSelected(true)

				handler.removeCallbacks(scrollbarHider)
				showScrollbar()
				if (showBubble && sectionIndexer != null) showBubble()

				fastScrollListener?.onFastScrollStart(this)

				setYPositions()
				return true
			}

			MotionEvent.ACTION_MOVE -> {
				setYPositions()
				return true
			}

			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				requestDisallowInterceptTouchEvent(false)
				setHandleSelected(false)

				if (hideScrollbar) handler.postDelayed(scrollbarHider, SCROLLBAR_HIDE_DELAY)
				if (!showBubbleAlways) hideBubble()

				fastScrollListener?.onFastScrollStop(this)

				return true
			}
		}

		return super.onTouchEvent(event)
	}

	/**
	 * Set the enabled state of this view.
	 *
	 * @param enabled True if this view is enabled, false otherwise
	 */
	override fun setEnabled(enabled: Boolean) {
		super.setEnabled(enabled)
		isVisible = enabled
	}

	/**
	 * Set the [ViewGroup.LayoutParams] associated with this view. These supply
	 * parameters to the *parent* of this view specifying how it should be arranged.
	 *
	 * @param params The [ViewGroup.LayoutParams] for this view, cannot be null
	 */
	override fun setLayoutParams(params: ViewGroup.LayoutParams) {
		params.width = LayoutParams.WRAP_CONTENT
		super.setLayoutParams(params)
	}

	/**
	 * Set the [ViewGroup.LayoutParams] associated with this view. These supply
	 * parameters to the *parent* of this view specifying how it should be arranged.
	 *
	 * @param viewGroup The parent [ViewGroup] for this view, cannot be null
	 */
	fun setLayoutParams(viewGroup: ViewGroup) {
		val recyclerViewId = recyclerView?.id ?: NO_ID
		val marginTop = resources.getDimensionPixelSize(R.dimen.fastscroll_scrollbar_margin_top)
		val marginBottom = resources.getDimensionPixelSize(R.dimen.fastscroll_scrollbar_margin_bottom)

		require(recyclerViewId != NO_ID) { "RecyclerView must have a view ID" }

		when (viewGroup) {
			is ConstraintLayout -> {
				val endId = if (recyclerView?.parent === parent) recyclerViewId else ConstraintSet.PARENT_ID
				val startId = id

				ConstraintSet().apply {
					clone(viewGroup)
					connect(startId, ConstraintSet.TOP, endId, ConstraintSet.TOP)
					connect(startId, ConstraintSet.BOTTOM, endId, ConstraintSet.BOTTOM)
					connect(startId, ConstraintSet.END, endId, ConstraintSet.END)
					applyTo(viewGroup)
				}

				layoutParams = (layoutParams as ConstraintLayout.LayoutParams).apply {
					height = 0
					setMargins(offset, marginTop, offset, marginBottom)
				}
			}

			is CoordinatorLayout -> layoutParams = (layoutParams as CoordinatorLayout.LayoutParams).apply {
				height = LayoutParams.MATCH_PARENT
				anchorGravity = GravityCompat.END
				anchorId = recyclerViewId
				setMargins(offset, marginTop, offset, marginBottom)
			}

			is FrameLayout -> layoutParams = (layoutParams as FrameLayout.LayoutParams).apply {
				height = LayoutParams.MATCH_PARENT
				gravity = GravityCompat.END
				setMargins(offset, marginTop, offset, marginBottom)
			}

			is RelativeLayout -> layoutParams = (layoutParams as RelativeLayout.LayoutParams).apply {
				height = 0
				addRule(RelativeLayout.ALIGN_TOP, recyclerViewId)
				addRule(RelativeLayout.ALIGN_BOTTOM, recyclerViewId)
				addRule(RelativeLayout.ALIGN_END, recyclerViewId)
				setMargins(offset, marginTop, offset, marginBottom)
			}

			else -> throw IllegalArgumentException("Parent ViewGroup must be a ConstraintLayout, CoordinatorLayout, FrameLayout, or RelativeLayout")
		}

		updateViewHeights()
	}

	/**
	 * Set the [RecyclerView] associated with this [FastScroller]. This allows the
	 * FastScroller to set its layout parameters and listen for scroll changes.
	 *
	 * @param recyclerView The [RecyclerView] to attach, cannot be null
	 * @see detachRecyclerView
	 */
	fun attachRecyclerView(recyclerView: RecyclerView) {
		if (this.recyclerView != null) {
			detachRecyclerView()
		}
		this.recyclerView = recyclerView

		if (parent is ViewGroup) {
			setLayoutParams(parent as ViewGroup)
		} else {
			val viewGroup = findValidParent(recyclerView)
			if (viewGroup != null) {
				viewGroup.addView(this)
				setLayoutParams(viewGroup)
			}
		}

		recyclerView.addOnScrollListener(scrollListener)

		// set initial positions for bubble and thumb
		post { setViewPositions(this.recyclerView?.scrollProportion ?: 0f) }
	}

	/**
	 * Clears references to the attached [RecyclerView] and stops listening for scroll changes.
	 *
	 * @see attachRecyclerView
	 */
	fun detachRecyclerView() {
		recyclerView?.removeOnScrollListener(scrollListener)
		recyclerView = null
	}

	/**
	 * Set a new [FastScrollListener] that will listen to fast scroll events.
	 *
	 * @param fastScrollListener The new [FastScrollListener] to set, or null to set none
	 */
	fun setFastScrollListener(fastScrollListener: FastScrollListener?) {
		this.fastScrollListener = fastScrollListener
	}

	/**
	 * Set a new [SectionIndexer] that provides section text for this [FastScroller].
	 *
	 * @param sectionIndexer The new [SectionIndexer] to set, or null to set none
	 */
	fun setSectionIndexer(sectionIndexer: SectionIndexer?) {
		this.sectionIndexer = sectionIndexer
	}

	/**
	 * Hide the scrollbar when not scrolling.
	 *
	 * @param hideScrollbar True to hide the scrollbar, false to show
	 */
	fun setHideScrollbar(hideScrollbar: Boolean) {
		if (this.hideScrollbar != hideScrollbar) {
			this.hideScrollbar = hideScrollbar
			binding.scrollbar.isGone = hideScrollbar
		}
	}

	/**
	 * Show the scroll track while scrolling.
	 *
	 * @param visible True to show scroll track, false to hide
	 */
	fun setTrackVisible(visible: Boolean) {
		binding.track.isVisible = visible
	}

	/**
	 * Set the color of the scroll track.
	 *
	 * @param color The color for the scroll track
	 */
	fun setTrackColor(@ColorInt color: Int) {
		if (trackImage == null) {
			trackImage = ContextCompat.getDrawable(context, R.drawable.fastscroll_track)
		}

		trackImage?.let {
			it.setTint(color)
			binding.track.setImageDrawable(it)
		}
	}

	/**
	 * Set the color of the scroll thumb.
	 *
	 * @param color The color for the scroll thumb
	 */
	fun setHandleColor(@ColorInt color: Int) {
		handleColor = color

		if (handleImage == null) {
			handleImage = ContextCompat.getDrawable(context, R.drawable.fastscroll_handle)
		}

		handleImage?.let {
			it.setTint(handleColor)
			binding.thumb.setImageDrawable(it)
		}
	}

	/**
	 * Show the section bubble while scrolling.
	 *
	 * @param visible True to show the bubble, false to hide
	 * @param always  True to always show the bubble, false to only show on thumb touch
	 */
	@JvmOverloads
	fun setBubbleVisible(visible: Boolean, always: Boolean = false) {
		showBubble = visible
		showBubbleAlways = visible && always
	}

	/**
	 * Set the background color of the section bubble.
	 *
	 * @param color The background color for the section bubble
	 */
	fun setBubbleColor(@ColorInt color: Int) {
		bubbleColor = color

		if (bubbleImage == null) {
			bubbleImage = ContextCompat.getDrawable(context, bubbleSize.drawableId)
		}

		bubbleImage?.let {
			it.setTint(bubbleColor)
			binding.bubble.background = it
		}
	}

	/**
	 * Set the text color of the section bubble.
	 *
	 * @param color The text color for the section bubble
	 */
	fun setBubbleTextColor(@ColorInt color: Int) = binding.bubble.setTextColor(color)

	/**
	 * Set the scaled pixel text size of the section bubble.
	 *
	 * @param size The scaled pixel text size for the section bubble
	 */
	fun setBubbleTextSize(size: Int) {
		binding.bubble.textSize = size.toFloat()
	}

	private fun getRecyclerViewTargetPosition(y: Float) = recyclerView?.let { recyclerView ->
		val itemCount = recyclerView.adapter?.itemCount ?: 0

		val proportion = when {
			binding.thumb.y == 0f -> 0f
			binding.thumb.y + handleHeight >= viewHeight - TRACK_SNAP_RANGE -> 1f
			else -> y / viewHeight.toFloat()
		}

		var scrolledItemCount = (proportion * itemCount).roundToInt()

		if (recyclerView.layoutManager.isLayoutReversed) {
			scrolledItemCount = itemCount - scrolledItemCount
		}

		if (itemCount > 0) scrolledItemCount.coerceIn(0, itemCount - 1) else 0
	} ?: 0

	private fun setRecyclerViewPosition(y: Float) {
		val layoutManager = recyclerView?.layoutManager ?: return
		val targetPos = getRecyclerViewTargetPosition(y)
		layoutManager.scrollToPosition(targetPos)
		if (showBubble) sectionIndexer?.let { bindBubble(it.getSectionText(context, targetPos)) }
	}

	private fun setViewPositions(y: Float) {
		bubbleHeight = binding.bubble.measuredHeight
		handleHeight = binding.thumb.measuredHeight

		val bubbleHandleHeight = bubbleHeight + handleHeight / 2f

		if (showBubble && viewHeight >= bubbleHandleHeight) {
			binding.bubble.y = (y - bubbleHeight).coerceIn(0f, viewHeight - bubbleHandleHeight)
		}

		if (viewHeight >= handleHeight) {
			binding.thumb.y = (y - handleHeight / 2).coerceIn(0f, viewHeight - handleHeight.toFloat())
		}
	}

	private fun updateViewHeights() {
		val measureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
		binding.bubble.measure(measureSpec, measureSpec)
		bubbleHeight = binding.bubble.measuredHeight
		binding.thumb.measure(measureSpec, measureSpec)
		handleHeight = binding.thumb.measuredHeight
	}

	private fun showBubble() {
		bubbleAnimator.show()
	}

	private fun hideBubble() {
		bubbleAnimator.hide()
	}

	private fun showScrollbar() {
		if (recyclerView?.run { canScrollVertically(1) || canScrollVertically(-1) } == true) {
			scrollbarAnimator.show()
		}
	}

	private fun hideScrollbar() {
		scrollbarAnimator.hide()
	}

	private fun setHandleSelected(selected: Boolean) {
		binding.thumb.isSelected = selected
		handleImage?.setTint(if (selected) bubbleColor else handleColor)
	}

	private fun TypedArray.getBubbleSize(@StyleableRes index: Int, defaultValue: BubbleSize): BubbleSize {
		val ordinal = getInt(index, -1)
		return BubbleSize.entries.getOrNull(ordinal) ?: defaultValue
	}

	private fun findValidParent(view: View): ViewGroup? = view.ancestors.firstNotNullOfOrNull { p ->
		if (p is FrameLayout || p is ConstraintLayout || p is CoordinatorLayout || p is RelativeLayout) {
			p as ViewGroup
		} else {
			null
		}
	}

	private fun bindBubble(text: CharSequence?) {
		binding.bubble.text = text
		binding.bubble.alpha = if (text.isNullOrEmpty()) 0f else 1f
	}

	private val BubbleSize.textSize
		@Px get() = resources.getDimension(textSizeId)

	interface FastScrollListener {

		fun onFastScrollStart(fastScroller: FastScroller)

		fun onFastScrollStop(fastScroller: FastScroller)
	}

	interface SectionIndexer {

		fun getSectionText(context: Context, position: Int): CharSequence?
	}
}
