/*
 * Copyright 2022 Randy Webster. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.koitharu.kotatsu.base.ui.widgets

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.widget.*
import android.widget.RelativeLayout.*
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StyleableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.utils.ext.*
import org.koitharu.kotatsu.utils.ext.getCompatDrawable
import org.koitharu.kotatsu.utils.ext.isLayoutReversed
import org.koitharu.kotatsu.utils.ext.setCompatTint
import org.koitharu.kotatsu.utils.ext.wrap
import kotlin.math.roundToInt

private const val BUBBLE_ANIM_DURATION = 100L
private const val SCROLLBAR_ANIM_DURATION = 300L
private const val SCROLLBAR_HIDE_DELAY = 1000L
private const val TRACK_SNAP_RANGE = 5

@Suppress("MemberVisibilityCanBePrivate", "unused")
class FastScroller : LinearLayout {

	enum class Size(@DrawableRes val drawableId: Int, @DimenRes val textSizeId: Int) {
		NORMAL(R.drawable.fastscroll_bubble, R.dimen.fastscroll_bubble_text_size),
		SMALL(R.drawable.fastscroll_bubble_small, R.dimen.fastscroll_bubble_text_size_small)
	}

	private val Size.textSize get() = resources.getDimension(textSizeId)

	private val bubbleView: TextView by lazy { findViewById(R.id.fastscroll_bubble) }
	private val handleView: ImageView by lazy { findViewById(R.id.fastscroll_handle) }
	private val trackView: ImageView by lazy { findViewById(R.id.fastscroll_track) }
	private val scrollbar: View by lazy { findViewById(R.id.fastscroll_scrollbar) }

	private val scrollbarPaddingEnd by lazy {
		resources.getDimensionPixelSize(R.dimen.fastscroll_scrollbar_padding_end).toFloat()
	}

	@ColorInt
	private var bubbleColor = 0
	@ColorInt
	private var handleColor = 0

	private var bubbleHeight = 0
	private var handleHeight = 0
	private var viewHeight = 0
	private var hideScrollbar = true
	private var showBubble = true
	private var showBubbleAlways = false
	private var bubbleSize = Size.NORMAL
	private var bubbleImage: Drawable? = null
	private var handleImage: Drawable? = null
	private var trackImage: Drawable? = null
	private var recyclerView: RecyclerView? = null
	private var swipeRefreshLayout: SwipeRefreshLayout? = null
	private var scrollbarAnimator: ViewPropertyAnimator? = null
	private var bubbleAnimator: ViewPropertyAnimator? = null

	private var fastScrollListener: FastScrollListener? = null
	private var sectionIndexer: SectionIndexer? = null

	private val scrollbarHider = Runnable {
		hideBubble()
		hideScrollbar()
	}

	private val alphaAnimatorListener = object : AnimatorListenerAdapter() {
		/* adapter required for new alpha value to stick */
	}

	private val scrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
		override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
			if (!handleView.isSelected && isEnabled) {
				val y = recyclerView.scrollProportion
				setViewPositions(y)

				if (showBubbleAlways) {
					val targetPos = getRecyclerViewTargetPosition(y)
					sectionIndexer?.let { bubbleView.text = it.getSectionText(targetPos) }
				}
			}

			swipeRefreshLayout?.let {
				val firstVisibleItem = recyclerView.layoutManager.firstVisibleItemPosition
				val topPosition = if (recyclerView.childCount == 0) 0 else recyclerView.getChildAt(0).top
				it.isEnabled = firstVisibleItem == 0 && topPosition >= 0
			}
		}

		override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
			super.onScrollStateChanged(recyclerView, newState)

			if (isEnabled) {
				when (newState) {
					RecyclerView.SCROLL_STATE_DRAGGING -> {
						handler.removeCallbacks(scrollbarHider)
						scrollbarAnimator?.cancel()

						if (!scrollbar.isVisible) showScrollbar()
						if (showBubbleAlways && sectionIndexer != null) showBubble()
					}
					RecyclerView.SCROLL_STATE_IDLE -> if (hideScrollbar && !handleView.isSelected) {
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

	@JvmOverloads
	constructor(context: Context, size: Size = Size.NORMAL) : super(context) {
		context.layout(size = size)
		layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
	}

	@JvmOverloads
	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {
		context.layout(attrs)
		layoutParams = attrs?.let { generateLayoutParams(it) } ?: LayoutParams(
			LayoutParams.WRAP_CONTENT,
			LayoutParams.MATCH_PARENT
		)
	}

	override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) = super.onSizeChanged(w, h, oldW, oldH).also {
		viewHeight = h
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(event: MotionEvent): Boolean {
		val setYPositions: () -> Unit = {
			val y = event.y
			setViewPositions(y)
			setRecyclerViewPosition(y)
		}

		when (event.action) {
			MotionEvent.ACTION_DOWN -> {
				if (event.x < handleView.x - scrollbar.compatPaddingStart) return false

				requestDisallowInterceptTouchEvent(true)
				setHandleSelected(true)

				handler.removeCallbacks(scrollbarHider)
				scrollbarAnimator?.cancel()
				bubbleAnimator?.cancel()

				if (!scrollbar.isVisible) showScrollbar()
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
	override fun setEnabled(enabled: Boolean) = super.setEnabled(enabled).also {
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
					setMargins(0, marginTop, 0, marginBottom)
				}
			}
			is CoordinatorLayout -> layoutParams = (layoutParams as CoordinatorLayout.LayoutParams).apply {
				height = LayoutParams.MATCH_PARENT
				anchorGravity = GravityCompat.END
				anchorId = recyclerViewId
				setMargins(0, marginTop, 0, marginBottom)
			}
			is FrameLayout -> layoutParams = (layoutParams as FrameLayout.LayoutParams).apply {
				height = LayoutParams.MATCH_PARENT
				gravity = GravityCompat.END
				setMargins(0, marginTop, 0, marginBottom)
			}
			is RelativeLayout -> layoutParams = (layoutParams as RelativeLayout.LayoutParams).apply {
				height = 0
				addRule(ALIGN_TOP, recyclerViewId)
				addRule(ALIGN_BOTTOM, recyclerViewId)
				addRule(ALIGN_END, recyclerViewId)
				setMargins(0, marginTop, 0, marginBottom)
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
		this.recyclerView = recyclerView

		if (parent is ViewGroup) {
			setLayoutParams(parent as ViewGroup)
		} else if (recyclerView.parent is ViewGroup) {
			val viewGroup = recyclerView.parent as ViewGroup
			viewGroup.addView(this)
			setLayoutParams(viewGroup)
		}

		recyclerView.addOnScrollListener(scrollListener)

		// set initial positions for bubble and handle
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
	 * Set a [SwipeRefreshLayout] to disable when the [RecyclerView] is scrolled away from the top.
	 *
	 * Required when SDK target precedes [VERSION_CODES.LOLLIPOP], otherwise use
	 * [setNestedScrollingEnabled(true)][View.setNestedScrollingEnabled].
	 *
	 * @param swipeRefreshLayout The [SwipeRefreshLayout] to set, or null to set none
	 */
	fun setSwipeRefreshLayout(swipeRefreshLayout: SwipeRefreshLayout?) {
		this.swipeRefreshLayout = swipeRefreshLayout
	}

	/**
	 * Hide the scrollbar when not scrolling.
	 *
	 * @param hideScrollbar True to hide the scrollbar, false to show
	 */
	fun setHideScrollbar(hideScrollbar: Boolean) {
		if (this.hideScrollbar != hideScrollbar) {
			scrollbar.isVisible = !hideScrollbar.also { this.hideScrollbar = it }
		}
	}

	/**
	 * Show the scroll track while scrolling.
	 *
	 * @param visible True to show scroll track, false to hide
	 */
	fun setTrackVisible(visible: Boolean) {
		trackView.isVisible = visible
	}

	/**
	 * Set the color of the scroll track.
	 *
	 * @param color The color for the scroll track
	 */
	fun setTrackColor(@ColorInt color: Int) {
		if (trackImage == null) {
			context.getCompatDrawable(R.drawable.fastscroll_track)?.let { trackImage = it.wrap().mutate() }
		}

		trackImage?.let {
			it.setCompatTint(color)
			trackView.setImageDrawable(it)
		}
	}

	/**
	 * Set the color of the scroll handle.
	 *
	 * @param color The color for the scroll handle
	 */
	fun setHandleColor(@ColorInt color: Int) {
		handleColor = color

		if (handleImage == null) {
			context.getCompatDrawable(R.drawable.fastscroll_handle)?.let { handleImage = it.wrap().mutate() }
		}

		handleImage?.let {
			it.setCompatTint(handleColor)
			handleView.setImageDrawable(it)
		}
	}

	/**
	 * Show the section bubble while scrolling.
	 *
	 * @param visible True to show the bubble, false to hide
	 * @param always  True to always show the bubble, false to only show on handle touch
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
			context.getCompatDrawable(bubbleSize.drawableId)?.let { bubbleImage = it.wrap().mutate() }
		}

		bubbleImage?.let {
			it.setCompatTint(bubbleColor)
			bubbleView.background = it
		}
	}

	/**
	 * Set the text color of the section bubble.
	 *
	 * @param color The text color for the section bubble
	 */
	fun setBubbleTextColor(@ColorInt color: Int) = bubbleView.setTextColor(color)

	/**
	 * Set the scaled pixel text size of the section bubble.
	 *
	 * @param size The scaled pixel text size for the section bubble
	 */
	fun setBubbleTextSize(size: Int) {
		bubbleView.textSize = size.toFloat()
	}

	private fun getRecyclerViewTargetPosition(y: Float) = recyclerView?.let { recyclerView ->
		val itemCount = recyclerView.adapter?.itemCount ?: 0

		val proportion = when {
			handleView.y == 0f -> 0f
			handleView.y + handleHeight >= viewHeight - TRACK_SNAP_RANGE -> 1f
			else -> y / viewHeight.toFloat()
		}

		var scrolledItemCount = (proportion * itemCount).roundToInt()

		if (recyclerView.layoutManager.isLayoutReversed) {
			scrolledItemCount = itemCount - scrolledItemCount
		}

		if (itemCount > 0) scrolledItemCount.coerceIn(0, itemCount - 1) else 0
	} ?: 0

	private fun setRecyclerViewPosition(y: Float) {
		recyclerView?.layoutManager?.let { layoutManager ->
			val targetPos = getRecyclerViewTargetPosition(y)
			layoutManager.scrollToPosition(targetPos)
			if (showBubble) sectionIndexer?.let { bubbleView.text = it.getSectionText(targetPos) }
		}
	}

	private fun setViewPositions(y: Float) {
		bubbleHeight = bubbleView.measuredHeight
		handleHeight = handleView.measuredHeight

		val bubbleHandleHeight = bubbleHeight + handleHeight / 2f

		if (showBubble && viewHeight >= bubbleHandleHeight) {
			bubbleView.y = (y - bubbleHeight).coerceIn(0f, viewHeight - bubbleHandleHeight)
		}

		if (viewHeight >= handleHeight) {
			handleView.y = (y - handleHeight / 2).coerceIn(0f, viewHeight - handleHeight.toFloat())
		}
	}

	private fun updateViewHeights() {
		val measureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
		bubbleView.measure(measureSpec, measureSpec)
		bubbleHeight = bubbleView.measuredHeight
		handleView.measure(measureSpec, measureSpec)
		handleHeight = handleView.measuredHeight
	}

	private fun showBubble() {
		if (!bubbleView.isVisible) {
			bubbleView.isVisible = true
			bubbleAnimator = bubbleView.animate().alpha(1f)
				.setDuration(BUBBLE_ANIM_DURATION)
				.setListener(alphaAnimatorListener)
		}
	}

	private fun hideBubble() {
		if (bubbleView.isVisible) {
			bubbleAnimator = bubbleView.animate().alpha(0f)
				.setDuration(BUBBLE_ANIM_DURATION)
				.setListener(object : AnimatorListenerAdapter() {
					override fun onAnimationEnd(animation: Animator) {
						super.onAnimationEnd(animation)
						bubbleView.isVisible = false
						bubbleAnimator = null
					}

					override fun onAnimationCancel(animation: Animator) {
						super.onAnimationCancel(animation)
						bubbleView.isVisible = false
						bubbleAnimator = null
					}
				})
		}
	}

	private fun showScrollbar() {
		if ((recyclerView?.computeVerticalScrollRange() ?: (0 - viewHeight)) > 0) {
			scrollbar.translationX = scrollbarPaddingEnd
			scrollbar.isVisible = true
			scrollbarAnimator = scrollbar.animate().translationX(0f).alpha(1f)
				.setDuration(SCROLLBAR_ANIM_DURATION)
				.setListener(alphaAnimatorListener)
		}
	}

	private fun hideScrollbar() {
		scrollbarAnimator = scrollbar.animate().translationX(scrollbarPaddingEnd).alpha(0f)
			.setDuration(SCROLLBAR_ANIM_DURATION)
			.setListener(object : AnimatorListenerAdapter() {
				override fun onAnimationEnd(animation: Animator) {
					super.onAnimationEnd(animation)
					scrollbar.isVisible = false
					scrollbarAnimator = null
				}

				override fun onAnimationCancel(animation: Animator) {
					super.onAnimationCancel(animation)
					scrollbar.isVisible = false
					scrollbarAnimator = null
				}
			})
	}

	private fun setHandleSelected(selected: Boolean) {
		handleView.isSelected = selected
		handleImage?.setCompatTint(if (selected) bubbleColor else handleColor)
	}

	private fun TypedArray.getSize(@StyleableRes index: Int, defValue: Int) = getInt(index, defValue).let { ordinal ->
		Size.values().find { it.ordinal == ordinal } ?: Size.NORMAL
	}

	private fun Context.layout(attrs: AttributeSet? = null, size: Size = Size.NORMAL) {
		inflate(this, R.layout.fast_scroller, this@FastScroller)

		clipChildren = false
		orientation = HORIZONTAL

		@ColorInt var bubbleColor = Color.GRAY
		@ColorInt var handleColor = Color.DKGRAY
		@ColorInt var trackColor = Color.LTGRAY
		@ColorInt var textColor = Color.WHITE

		var showTrack = false
		var textSize = size.textSize

		withStyledAttributes(attrs, R.styleable.FastScroller) {
			bubbleColor = getColor(R.styleable.FastScroller_bubbleColor, bubbleColor)
			handleColor = getColor(R.styleable.FastScroller_handleColor, handleColor)
			trackColor = getColor(R.styleable.FastScroller_trackColor, trackColor)
			textColor = getColor(R.styleable.FastScroller_bubbleTextColor, textColor)
			hideScrollbar = getBoolean(R.styleable.FastScroller_hideScrollbar, hideScrollbar)
			showBubble = getBoolean(R.styleable.FastScroller_showBubble, showBubble)
			showBubbleAlways = getBoolean(R.styleable.FastScroller_showBubbleAlways, showBubbleAlways)
			showTrack = getBoolean(R.styleable.FastScroller_showTrack, showTrack)
			bubbleSize = getSize(R.styleable.FastScroller_bubbleSize, size.ordinal)
			textSize = getDimension(R.styleable.FastScroller_bubbleTextSize, bubbleSize.textSize)
		}

		setTrackColor(trackColor)
		setHandleColor(handleColor)
		setBubbleColor(bubbleColor)
		setBubbleTextColor(textColor)
		setHideScrollbar(hideScrollbar)
		setBubbleVisible(showBubble, showBubbleAlways)
		setTrackVisible(showTrack)

		bubbleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
	}

	interface FastScrollListener {

		fun onFastScrollStart(fastScroller: FastScroller)

		fun onFastScrollStop(fastScroller: FastScroller)
	}

	interface SectionIndexer {

		fun getSectionText(position: Int): CharSequence
	}
}