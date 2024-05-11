package org.koitharu.kotatsu.reader.ui.pager.doublepage

import android.util.DisplayMetrics
import android.view.View
import android.view.animation.Interpolator
import android.widget.Scroller
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller.ScrollVectorProvider
import androidx.recyclerview.widget.SnapHelper
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class DoublePageSnapHelper : SnapHelper() {

	private lateinit var recyclerView: RecyclerView

	// Total number of items in a block of view in the RecyclerView
	private var blockSize = 2

	// Maximum number of positions to move on a fling.
	private var maxPositionsToMove = 0

	// Width of a RecyclerView item if orientation is horizontal; height of the item if vertical
	private var itemDimension = 0

	// Maxim blocks to move during most vigorous fling.
	private val maxFlingBlocks = 2

	// When snapping, used to determine direction of snap.
	private var priorFirstPosition = RecyclerView.NO_POSITION

	// Our private scroller
	private var scroller: Scroller? = null

	// Horizontal/vertical layout helper
	private lateinit var orientationHelper: OrientationHelper

	// LTR/RTL helper
	private lateinit var layoutDirectionHelper: LayoutDirectionHelper

	private val snapInterpolator = Interpolator { input ->
		var t = input
		t -= 1.0f
		t * t * t + 1.0f
	}

	@Throws(IllegalStateException::class)
	override fun attachToRecyclerView(target: RecyclerView?) {
		if (target != null) {
			recyclerView = target
			val layoutManager = recyclerView.layoutManager as LinearLayoutManager
			check(layoutManager.canScrollHorizontally()) { "RecyclerView must be scrollable" }
			orientationHelper = OrientationHelper.createHorizontalHelper(layoutManager)
			layoutDirectionHelper = LayoutDirectionHelper(recyclerView.layoutDirection)
			scroller = Scroller(target.context, snapInterpolator)
			initItemDimensionIfNeeded(layoutManager)
		}
		super.attachToRecyclerView(recyclerView)
	}

	override fun calculateDistanceToFinalSnap(
		layoutManager: RecyclerView.LayoutManager,
		targetView: View
	): IntArray {
		val out = IntArray(2)
		if (layoutManager.canScrollHorizontally()) {
			out[0] = layoutDirectionHelper.getScrollToAlignView(targetView)
		}
		if (layoutManager.canScrollVertically()) {
			out[1] = layoutDirectionHelper.getScrollToAlignView(targetView)
		}
		return out
	}

	// We are flinging and need to know where we are heading.
	override fun findTargetSnapPosition(
		layoutManager: RecyclerView.LayoutManager,
		velocityX: Int, velocityY: Int
	): Int {
		val lm = layoutManager as LinearLayoutManager
		initItemDimensionIfNeeded(layoutManager)
		scroller!!.fling(0, 0, velocityX, velocityY, Int.MIN_VALUE, Int.MAX_VALUE, Int.MIN_VALUE, Int.MAX_VALUE)
		if (velocityX != 0) {
			return layoutDirectionHelper
				.getPositionsToMove(lm, scroller!!.finalX, itemDimension)
		}
		return if (velocityY != 0) {
			layoutDirectionHelper
				.getPositionsToMove(lm, scroller!!.finalY, itemDimension)
		} else RecyclerView.NO_POSITION
	}

	// We have scrolled to the neighborhood where we will snap. Determine the snap position.
	override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
		// Snap to a view that is either 1) toward the bottom of the data and therefore on screen,
		// or, 2) toward the top of the data and may be off-screen.
		val snapPos: Int = calcTargetPosition(layoutManager as LinearLayoutManager)
		return if (snapPos == RecyclerView.NO_POSITION) null else layoutManager.findViewByPosition(snapPos)
	}

	// Does the heavy lifting for findSnapView.
	private fun calcTargetPosition(layoutManager: LinearLayoutManager): Int {
		val snapPos: Int
		val firstVisiblePos = layoutManager.findFirstVisibleItemPosition()
		if (firstVisiblePos == RecyclerView.NO_POSITION) {
			return RecyclerView.NO_POSITION
		}
		initItemDimensionIfNeeded(layoutManager)
		if (firstVisiblePos >= priorFirstPosition) {
			// Scrolling toward bottom of data
			val firstCompletePosition = layoutManager.findFirstCompletelyVisibleItemPosition()
			snapPos = if (firstCompletePosition != RecyclerView.NO_POSITION
				&& firstCompletePosition % blockSize == 0
			) {
				firstCompletePosition
			} else {
				roundDownToBlockSize(firstVisiblePos + blockSize)
			}
		} else {
			// Scrolling toward top of data
			snapPos = roundDownToBlockSize(firstVisiblePos)
			// Check to see if target view exists. If it doesn't, force a smooth scroll.
			// SnapHelper only snaps to existing views and will not scroll to a non-existent one.
			// If limiting fling to single block, then the following is not needed since the
			// views are likely to be in the RecyclerView pool.
			if (layoutManager.findViewByPosition(snapPos) == null) {
				val toScroll: IntArray = layoutDirectionHelper.calculateDistanceToScroll(layoutManager, snapPos)
				recyclerView.smoothScrollBy(toScroll[0], toScroll[1], snapInterpolator)
			}
		}
		priorFirstPosition = firstVisiblePos
		return snapPos
	}

	private fun initItemDimensionIfNeeded(layoutManager: RecyclerView.LayoutManager) {
		if (itemDimension != 0) {
			return
		}
		val child: View = layoutManager.getChildAt(0) ?: return
		if (layoutManager.canScrollHorizontally()) {
			itemDimension = child.width
			blockSize = getSpanCount(layoutManager) * (recyclerView.width / itemDimension)
		} else if (layoutManager.canScrollVertically()) {
			itemDimension = child.height
			blockSize = getSpanCount(layoutManager) * (recyclerView.height / itemDimension)
		}
		maxPositionsToMove = blockSize * maxFlingBlocks
	}

	private fun getSpanCount(layoutManager: RecyclerView.LayoutManager): Int {
		return if (layoutManager is GridLayoutManager) layoutManager.spanCount else 1
	}

	private fun roundDownToBlockSize(trialPosition: Int): Int {
		return trialPosition and 1.inv()
	}

	private fun roundUpToBlockSize(trialPosition: Int): Int {
		return roundDownToBlockSize(trialPosition + blockSize - 1)
	}

	override fun createScroller(layoutManager: RecyclerView.LayoutManager): RecyclerView.SmoothScroller? {
		return if (layoutManager !is ScrollVectorProvider) {
			null
		} else object : LinearSmoothScroller(recyclerView.context) {
			override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
				val snapDistances = calculateDistanceToFinalSnap(
					recyclerView.layoutManager!!,
					targetView,
				)
				val dx = snapDistances[0]
				val dy = snapDistances[1]
				val time = calculateTimeForDeceleration(
					max(abs(dx.toDouble()), abs(dy.toDouble()))
						.toInt(),
				)
				if (time > 0) {
					action.update(dx, dy, time, snapInterpolator)
				}
			}

			override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
				return 40f / displayMetrics.densityDpi
			}
		}
	}

	/*
		Helper class that handles calculations for LTR and RTL layouts.
	 */
	private inner class LayoutDirectionHelper(direction: Int) {

		// Is the layout an RTL one?
		private val isRTL = direction == View.LAYOUT_DIRECTION_RTL

		/*
			Calculate the amount of scroll needed to align the target view with the layout edge.
		 */
		fun getScrollToAlignView(targetView: View?): Int {
			return if (isRTL) {
				orientationHelper.getDecoratedEnd(targetView) - recyclerView.width
			} else {
				orientationHelper.getDecoratedStart(targetView)
			}
		}

		/**
		 * Calculate the distance to final snap position when the view corresponding to the snap
		 * position is not currently available.
		 *
		 * @param layoutManager LinearLayoutManager or descendant class
		 * @param targetPos     - Adapter position to snap to
		 * @return int[2] {x-distance in pixels, y-distance in pixels}
		 */
		fun calculateDistanceToScroll(layoutManager: LinearLayoutManager, targetPos: Int): IntArray {
			val out = IntArray(2)
			val firstVisiblePos = layoutManager.findFirstVisibleItemPosition()
			if (layoutManager.canScrollHorizontally()) {
				if (targetPos <= firstVisiblePos) { // scrolling toward top of data
					if (isRTL) {
						val lastView = layoutManager.findViewByPosition(layoutManager.findLastVisibleItemPosition())
						out[0] = (orientationHelper.getDecoratedEnd(lastView)
							+ (firstVisiblePos - targetPos) * itemDimension)
					} else {
						val firstView = layoutManager.findViewByPosition(firstVisiblePos)
						out[0] = (orientationHelper.getDecoratedStart(firstView)
							- (firstVisiblePos - targetPos) * itemDimension)
					}
				}
			}
			if (layoutManager.canScrollVertically()) {
				if (targetPos <= firstVisiblePos) { // scrolling toward top of data
					val firstView = layoutManager.findViewByPosition(firstVisiblePos)
					out[1] = firstView!!.top - (firstVisiblePos - targetPos) * itemDimension
				}
			}
			return out
		}

		/*
			Calculate the number of positions to move in the RecyclerView given a scroll amount
			and the size of the items to be scrolled. Return integral multiple of mBlockSize not
			equal to zero.
		 */
		fun getPositionsToMove(llm: LinearLayoutManager, scroll: Int, itemSize: Int): Int {
			var positionsToMove: Int
			positionsToMove = roundUpToBlockSize(abs((scroll.toDouble()) / itemSize).roundToInt())
			if (positionsToMove < blockSize) {
				// Must move at least one block
				positionsToMove = blockSize
			} else if (positionsToMove > maxPositionsToMove) {
				// Clamp number of positions to move, so we don't get wild flinging.
				positionsToMove = maxPositionsToMove
			}
			if (scroll < 0) {
				positionsToMove *= -1
			}
			if (isRTL) {
				positionsToMove *= -1
			}
			return if (layoutDirectionHelper.isDirectionToBottom(scroll < 0)) {
				// Scrolling toward the bottom of data.
				roundDownToBlockSize(llm.findFirstVisibleItemPosition()) + positionsToMove
			} else {
				roundDownToBlockSize(llm.findLastVisibleItemPosition()) + positionsToMove
			}
			// Scrolling toward the top of the data.
		}

		fun isDirectionToBottom(velocityNegative: Boolean): Boolean {
			return if (isRTL) velocityNegative else !velocityNegative
		}
	}
}
