package org.koitharu.kotatsu.reader.ui.panel

import android.graphics.PointF
import android.graphics.RectF
import android.view.ViewTreeObserver
import androidx.annotation.MainThread
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.koitharu.kotatsu.reader.domain.panel.Panel
import org.koitharu.kotatsu.reader.domain.panel.PanelDetectionResult
import org.koitharu.kotatsu.reader.domain.panel.PanelSequence
import org.koitharu.kotatsu.reader.domain.panel.RectInt
import kotlin.math.max
import kotlin.math.min

/**
 * Controller responsible for aligning the reader viewport with a detected panel.
 * It translates panel metadata into scale/center commands for [SubsamplingScaleImageView]
 * and provides callbacks for optional UI highlights.
 */
class PanelZoomController(
    private val imageView: SubsamplingScaleImageView,
    private val highlighter: PanelHighlightListener? = null,
    private val config: PanelZoomConfig = PanelZoomConfig()
) {

    private var currentSequence: PanelSequence? = null
    private var currentIndex: Int = -1
    private var pendingIndex: Int? = null

    private val readinessListener = ViewTreeObserver.OnGlobalLayoutListener {
        attemptPendingFocus()
    }

    init {
        imageView.viewTreeObserver.addOnGlobalLayoutListener(readinessListener)
    }

    fun dispose() {
        imageView.viewTreeObserver.removeOnGlobalLayoutListener(readinessListener)
    }

    @MainThread
    fun setResult(result: PanelDetectionResult?) {
        currentSequence = result?.primary
        currentIndex = -1
        pendingIndex = null
    }

    @MainThread
    fun focus(panelIndex: Int, animate: Boolean = true) {
        val sequence = currentSequence ?: return
        val panel = sequence.panels.getOrNull(panelIndex) ?: return
        if (!imageViewReady()) {
            pendingIndex = panelIndex
            return
        }
        currentIndex = panelIndex
        pendingIndex = null

        val targetScale = calculateTargetScale(panel.bounds)
        val targetCenter = PointF(panel.centroid.x, panel.centroid.y)

        if (animate && imageView.isReady) {
            imageView.animateScaleAndCenter(targetScale, targetCenter)
                ?.withDuration(config.animationDurationMs)
        } else {
            imageView.setScaleAndCenter(targetScale, targetCenter)
        }

        dispatchHighlight(panel)
    }

    fun focusNext(loop: Boolean = false): Boolean {
        val sequence = currentSequence ?: return false
        if (sequence.panels.isEmpty()) {
            return false
        }
        val nextIndex = when {
            currentIndex < 0 -> 0
            currentIndex + 1 < sequence.panels.size -> currentIndex + 1
            loop -> 0
            else -> return false
        }
        focus(nextIndex)
        return true
    }

    fun focusPrevious(loop: Boolean = false): Boolean {
        val sequence = currentSequence ?: return false
        if (sequence.panels.isEmpty()) {
            return false
        }
        val nextIndex = when {
            currentIndex <= 0 && !loop -> return false
            currentIndex <= 0 -> sequence.panels.lastIndex
            else -> currentIndex - 1
        }
        focus(nextIndex)
        return true
    }

    fun currentPanel(): Panel? = currentSequence?.panels?.getOrNull(currentIndex)

    private fun attemptPendingFocus() {
        val index = pendingIndex ?: return
        if (imageViewReady()) {
            focus(index)
        }
    }

    private fun imageViewReady(): Boolean {
        return imageView.isReady && imageView.width > 0 && imageView.height > 0
    }

    private fun calculateTargetScale(bounds: RectInt): Float {
        val viewWidth = (imageView.width - config.viewportPaddingPx * 2).coerceAtLeast(1)
        val viewHeight = (imageView.height - config.viewportPaddingPx * 2).coerceAtLeast(1)
        val panelWidth = bounds.width.toFloat().coerceAtLeast(1f)
        val panelHeight = bounds.height.toFloat().coerceAtLeast(1f)

        val scale = min(viewWidth / panelWidth, viewHeight / panelHeight) * config.scaleMultiplier
        val minScale = config.minScale ?: imageView.minScale
        val maxScale = config.maxScale ?: imageView.maxScale
        return scale.coerceIn(minScale, maxScale)
    }

    private fun dispatchHighlight(panel: Panel) {
        val listener = highlighter ?: return
        if (!imageView.isReady) {
            return
        }
        val viewRect = RectF()
        val sourceRect = RectF(
            panel.bounds.left.toFloat(),
            panel.bounds.top.toFloat(),
            panel.bounds.right.toFloat(),
            panel.bounds.bottom.toFloat()
        )
        imageView.sourceToViewRect(sourceRect, viewRect)
        listener.onPanelFocused(panel, viewRect)
    }
}

interface PanelHighlightListener {
    fun onPanelFocused(panel: Panel, viewRect: RectF)
}

data class PanelZoomConfig(
    val viewportPaddingPx: Int = 32,
    val scaleMultiplier: Float = 1.05f,
    val animationDurationMs: Long = 220,
    val minScale: Float? = null,
    val maxScale: Float? = null
)



