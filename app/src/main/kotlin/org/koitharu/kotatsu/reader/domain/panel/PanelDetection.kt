package org.koitharu.kotatsu.reader.domain.panel

import android.graphics.Bitmap
import java.util.Comparator

import kotlin.collections.ArrayDeque
import kotlin.math.max
import kotlin.math.min

/**
 * API surface for detaching panel detection logic from UI code.
 */
interface PanelDetector {

    suspend fun detect(request: PanelDetectionRequest): PanelDetectionResult
}

data class PanelDetectionRequest(
    val image: PanelImage,
    val pageIndex: Int = 0,
    val isDoublePage: Boolean = false,
    val preferredFlow: PanelFlow = PanelFlow.LeftToRight,
    val maxPanels: Int = DEFAULT_MAX_PANELS,
    val minPanelAreaRatio: Float = DEFAULT_MIN_PANEL_AREA_RATIO,
    val backgroundColor: Int? = null,
    val backgroundTolerance: Int = DEFAULT_BACKGROUND_TOLERANCE
) {
    init {
        require(image.width > 0 && image.height > 0) {
            "Image size must be positive: ${image.width} x ${image.height}"
        }
        require(maxPanels > 0) { "maxPanels must be > 0" }
        require(minPanelAreaRatio in 0f..1f) { "minPanelAreaRatio must be within 0..1" }
    }

    companion object {
        const val DEFAULT_MAX_PANELS = 12
        const val DEFAULT_BACKGROUND_TOLERANCE = 30
        const val DEFAULT_MIN_PANEL_AREA_RATIO = 0.0125f
    }
}

data class PanelDetectionResult(
    val primary: PanelSequence,
    val alternatives: List<PanelSequence> = emptyList(),
    val issues: List<PanelDetectionIssue> = emptyList()
)

data class PanelSequence(
    val pageIndex: Int,
    val size: SizeInt,
    val panels: List<Panel>,
    val flow: PanelFlow,
    val layoutType: PanelLayoutType,
    val stats: PanelDetectionStats = PanelDetectionStats()
) {

    val panelCount: Int get() = panels.size
    val isEmpty: Boolean get() = panels.isEmpty()
    val occupiedArea: Int get() = panels.sumOf { it.bounds.area }
}

data class Panel(
    val id: Int,
    val bounds: RectInt,
    val centroid: PointFloat,
    val rotation: Float = 0f,
    val weight: Int = bounds.area,
    val confidence: Float = 1f
)

data class PanelDetectionStats(
    val downscaleFactor: Float = 1f,
    val processedPixels: Int = 0,
    val elapsedMillis: Long = 0L
)

enum class PanelLayoutType {
    Detected,
    FallbackFullPage,
    FallbackVerticalSplit,
    FallbackHorizontalSplit
}

enum class PanelFlow {
    LeftToRight,
    RightToLeft,
    TopToBottom
}

sealed interface PanelDetectionIssue {
    val message: String

    data class Warning(override val message: String) : PanelDetectionIssue
    data class Error(override val message: String, val throwable: Throwable? = null) : PanelDetectionIssue
}

interface PanelImage {
    val width: Int
    val height: Int
    fun getPixel(x: Int, y: Int): Int
}

data class SizeInt(val width: Int, val height: Int) {
    init {
        require(width >= 0 && height >= 0) { "Invalid size: $width x $height" }
    }
}

data class RectInt(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    init {
        require(right > left && bottom > top) {
            "Invalid rect: ($left,$top,$right,$bottom)"
        }
    }

    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val area: Int get() = width * height

    fun contains(x: Int, y: Int): Boolean {
        return x in left until right && y in top until bottom
    }
}

data class PointFloat(val x: Float, val y: Float)

/**
 * Adaptive detector that downsamples a page, thresholds it and extracts connected components
 * before projecting them back into the original image space.
 */
class AdaptivePanelDetector(
    private val config: Config = Config()
) : PanelDetector {

    data class Config(
        val targetMaxDimension: Int = 1024,
        val minComponentPixels: Int = 48,
        val panelPaddingFraction: Float = 0.02f,
        val mergeOverlapThreshold: Float = 0.2f,
        val maxMergePasses: Int = 2
    )

    override suspend fun detect(request: PanelDetectionRequest): PanelDetectionResult {
        val startNanos = System.nanoTime()
        val sampled = sampleImage(request.image, config.targetMaxDimension)
        val totalSamples = sampled.width * sampled.height
        if (totalSamples == 0) {
            val stats = PanelDetectionStats(downscaleFactor = sampled.step.toFloat(), processedPixels = 0)
            val fallback = createFullPageFallback(request, SizeInt(sampled.originalWidth, sampled.originalHeight), stats)
            return PanelDetectionResult(fallback, createSplitFallbacks(request, fallback.size, stats), emptyList())
        }

        val threshold = computeOtsuThreshold(sampled.histogram, totalSamples)
        val backgroundLuma = request.backgroundColor?.let(::colorLuma) ?: estimateBackgroundLuma(sampled)
        val backgroundIsLight = backgroundLuma >= threshold
        val tolerance = request.backgroundTolerance.coerceIn(0, 255)
        val lower = (threshold - tolerance).coerceAtLeast(0)
        val upper = (threshold + tolerance).coerceAtMost(255)

        val mask = BooleanArray(totalSamples)
        for (index in 0 until totalSamples) {
            val value = sampled.luma[index]
            mask[index] = if (backgroundIsLight) {
                value <= lower
            } else {
                value >= upper
            }
        }

        val minPixelsByArea = ((request.minPanelAreaRatio * sampled.originalWidth * sampled.originalHeight) /
            (sampled.step * sampled.step)).toInt()
        val minComponentPixels = max(config.minComponentPixels, minPixelsByArea)

        val components = collectComponents(mask, sampled, minComponentPixels)
        val bounds = components
            .map { componentToRect(it, sampled, config.panelPaddingFraction) }
            .toMutableList()

        val merged = mergeBounds(bounds)
        val filtered = filterNested(merged)
            .sortedByDescending { it.area }
            .take(request.maxPanels)

        val stats = PanelDetectionStats(
            downscaleFactor = sampled.step.toFloat(),
            processedPixels = totalSamples,
            elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000
        )

        val pageSize = SizeInt(sampled.originalWidth, sampled.originalHeight)
        val panels = filtered.mapIndexed { index, rect ->
            Panel(
                id = index,
                bounds = rect,
                centroid = PointFloat(
                    (rect.left + rect.right - 1) * 0.5f,
                    (rect.top + rect.bottom - 1) * 0.5f
                ),
                confidence = (rect.area.toFloat() / (pageSize.width * pageSize.height).coerceAtLeast(1)).coerceIn(0f, 1f)
            )
        }

        if (panels.isEmpty()) {
            val fallback = createFullPageFallback(request, pageSize, stats)
            val alternatives = createSplitFallbacks(request, pageSize, stats)
            return PanelDetectionResult(
                primary = fallback,
                alternatives = alternatives,
                issues = listOf(PanelDetectionIssue.Warning("No panels detected; fallback layout used"))
            )
        }

        val primary = PanelSequence(
            pageIndex = request.pageIndex,
            size = pageSize,
            panels = panels.sortedWith(request.preferredFlow.comparator),
            flow = request.preferredFlow,
            layoutType = PanelLayoutType.Detected,
            stats = stats
        )

        val alternatives = mutableListOf<PanelSequence>()
        alternatives += createFullPageFallback(request, pageSize, stats)
        alternatives += createSplitFallbacks(request, pageSize, stats)

        return PanelDetectionResult(primary, alternatives.distinctBy { it.layoutType }, emptyList())
    }

    private fun createFullPageFallback(
        request: PanelDetectionRequest,
        size: SizeInt,
        stats: PanelDetectionStats
    ): PanelSequence {
        val bounds = RectInt(0, 0, size.width, size.height)
        val panel = Panel(
            id = 0,
            bounds = bounds,
            centroid = PointFloat(size.width * 0.5f, size.height * 0.5f)
        )
        return PanelSequence(
            pageIndex = request.pageIndex,
            size = size,
            panels = listOf(panel),
            flow = request.preferredFlow,
            layoutType = PanelLayoutType.FallbackFullPage,
            stats = stats
        )
    }

    private fun createSplitFallbacks(
        request: PanelDetectionRequest,
        size: SizeInt,
        stats: PanelDetectionStats
    ): List<PanelSequence> {
        val alternatives = mutableListOf<PanelSequence>()
        if (size.width >= size.height) {
            val midpoint = size.width / 2
            val left = RectInt(0, 0, midpoint, size.height)
            val right = RectInt(midpoint, 0, size.width, size.height)
            val panels = listOf(
                Panel(0, left, PointFloat(left.width * 0.5f, size.height * 0.5f)),
                Panel(1, right, PointFloat(midpoint + right.width * 0.5f, size.height * 0.5f))
            )
            alternatives += PanelSequence(
                pageIndex = request.pageIndex,
                size = size,
                panels = panels,
                flow = request.preferredFlow,
                layoutType = PanelLayoutType.FallbackVerticalSplit,
                stats = stats
            )
        }
        if (size.height > size.width) {
            val midpoint = size.height / 2
            val top = RectInt(0, 0, size.width, midpoint)
            val bottom = RectInt(0, midpoint, size.width, size.height)
            val panels = listOf(
                Panel(0, top, PointFloat(size.width * 0.5f, top.height * 0.5f)),
                Panel(1, bottom, PointFloat(size.width * 0.5f, midpoint + bottom.height * 0.5f))
            )
            alternatives += PanelSequence(
                pageIndex = request.pageIndex,
                size = size,
                panels = panels,
                flow = request.preferredFlow,
                layoutType = PanelLayoutType.FallbackHorizontalSplit,
                stats = stats
            )
        }
        return alternatives
    }

    private val PanelFlow.comparator: Comparator<Panel>
        get() = Comparator { first, second ->
            when (this) {
                PanelFlow.LeftToRight -> compareByRowThenColumn(first, second, ascendingColumns = true)
                PanelFlow.RightToLeft -> compareByRowThenColumn(first, second, ascendingColumns = false)
                PanelFlow.TopToBottom -> compareByColumnThenRow(first, second)
            }
        }

    private fun compareByRowThenColumn(a: Panel, b: Panel, ascendingColumns: Boolean): Int {
        val row = a.bounds.top.compareTo(b.bounds.top)
        if (row != 0) {
            return row
        }
        return if (ascendingColumns) {
            a.bounds.left.compareTo(b.bounds.left)
        } else {
            b.bounds.left.compareTo(a.bounds.left)
        }
    }

    private fun compareByColumnThenRow(a: Panel, b: Panel): Int {
        val column = a.bounds.left.compareTo(b.bounds.left)
        if (column != 0) {
            return column
        }
        return a.bounds.top.compareTo(b.bounds.top)
    }

    private data class SampledImage(
        val width: Int,
        val height: Int,
        val step: Int,
        val luma: IntArray,
        val histogram: IntArray,
        val originalWidth: Int,
        val originalHeight: Int
    )

    private data class Component(
        val minX: Int,
        val minY: Int,
        val maxX: Int,
        val maxY: Int,
        val pixels: Int
    )

    private fun sampleImage(image: PanelImage, targetMaxDimension: Int): SampledImage {
        val originalWidth = image.width
        val originalHeight = image.height
        val step = max(1, max(originalWidth, originalHeight) / targetMaxDimension)
        val sampleWidth = max(1, (originalWidth + step - 1) / step)
        val sampleHeight = max(1, (originalHeight + step - 1) / step)
        val luma = IntArray(sampleWidth * sampleHeight)
        val histogram = IntArray(256)

        for (sy in 0 until sampleHeight) {
            val yStart = sy * step
            val yEnd = min(originalHeight, yStart + step)
            for (sx in 0 until sampleWidth) {
                val xStart = sx * step
                val xEnd = min(originalWidth, xStart + step)
                var sum = 0
                var count = 0
                for (y in yStart until yEnd) {
                    for (x in xStart until xEnd) {
                        val lumaValue = colorLuma(image.getPixel(x, y))
                        sum += lumaValue
                        count++
                    }
                }
                val index = sy * sampleWidth + sx
                val value = if (count == 0) 255 else sum / count
                luma[index] = value
                histogram[value]++
            }
        }

        return SampledImage(
            width = sampleWidth,
            height = sampleHeight,
            step = step,
            luma = luma,
            histogram = histogram,
            originalWidth = originalWidth,
            originalHeight = originalHeight
        )
    }

    private fun collectComponents(mask: BooleanArray, sampled: SampledImage, minPixels: Int): List<Component> {
        val result = mutableListOf<Component>()
        val visited = BooleanArray(mask.size)
        val queue = ArrayDeque<Int>()
        val width = sampled.width
        val height = sampled.height

        for (index in mask.indices) {
            if (!mask[index] || visited[index]) continue
            queue.addLast(index)
            visited[index] = true
            var count = 0
            var minX = width
            var minY = height
            var maxX = -1
            var maxY = -1
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                val x = current % width
                val y = current / width
                count++
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y

                if (x > 0) {
                    val left = current - 1
                    if (!visited[left] && mask[left]) {
                        visited[left] = true
                        queue.addLast(left)
                    }
                }
                if (x + 1 < width) {
                    val right = current + 1
                    if (!visited[right] && mask[right]) {
                        visited[right] = true
                        queue.addLast(right)
                    }
                }
                if (y > 0) {
                    val up = current - width
                    if (!visited[up] && mask[up]) {
                        visited[up] = true
                        queue.addLast(up)
                    }
                }
                if (y + 1 < height) {
                    val down = current + width
                    if (!visited[down] && mask[down]) {
                        visited[down] = true
                        queue.addLast(down)
                    }
                }
            }
            if (count >= minPixels && maxX >= minX && maxY >= minY) {
                result += Component(minX, minY, maxX, maxY, count)
            }
        }
        return result
    }

    private fun componentToRect(component: Component, sampled: SampledImage, paddingFraction: Float): RectInt {
        val step = sampled.step
        val originalWidth = sampled.originalWidth
        val originalHeight = sampled.originalHeight

        val left = (component.minX * step).coerceAtLeast(0)
        val top = (component.minY * step).coerceAtLeast(0)
        val right = min(originalWidth, (component.maxX + 1) * step)
        val bottom = min(originalHeight, (component.maxY + 1) * step)

        val paddingX = max(1, ((right - left) * paddingFraction).toInt())
        val paddingY = max(1, ((bottom - top) * paddingFraction).toInt())

        return RectInt(
            left = (left - paddingX).coerceAtLeast(0),
            top = (top - paddingY).coerceAtLeast(0),
            right = (right + paddingX).coerceAtMost(originalWidth),
            bottom = (bottom + paddingY).coerceAtMost(originalHeight)
        )
    }

    private fun mergeBounds(bounds: MutableList<RectInt>): MutableList<RectInt> {
        if (bounds.size < 2) return bounds
        var pass = 0
        while (pass < config.maxMergePasses) {
            var merged = false
            outer@ for (i in 0 until bounds.size) {
                for (j in i + 1 until bounds.size) {
                    val a = bounds[i]
                    val b = bounds[j]
                    val overlap = intersectionRatio(a, b)
                    if (overlap >= config.mergeOverlapThreshold) {
                        val mergedRect = RectInt(
                            left = min(a.left, b.left),
                            top = min(a.top, b.top),
                            right = max(a.right, b.right),
                            bottom = max(a.bottom, b.bottom)
                        )
                        bounds[i] = mergedRect
                        bounds.removeAt(j)
                        merged = true
                        break@outer
                    }
                }
            }
            if (!merged) break
            pass++
        }
        return bounds
    }

    private fun filterNested(bounds: List<RectInt>): List<RectInt> {
        if (bounds.size <= 1) return bounds
        val result = mutableListOf<RectInt>()
        for (candidate in bounds) {
            val contained = bounds.any { other ->
                other !== candidate && other.contains(candidate, tolerance = 6)
            }
            if (!contained) {
                result += candidate
            }
        }
        return result
    }

    private fun RectInt.contains(other: RectInt, tolerance: Int): Boolean {
        return other.left >= left - tolerance &&
            other.top >= top - tolerance &&
            other.right <= right + tolerance &&
            other.bottom <= bottom + tolerance
    }

    private fun intersectionRatio(a: RectInt, b: RectInt): Float {
        val left = max(a.left, b.left)
        val top = max(a.top, b.top)
        val right = min(a.right, b.right)
        val bottom = min(a.bottom, b.bottom)
        if (right <= left || bottom <= top) {
            return 0f
        }
        val intersection = (right - left) * (bottom - top)
        val union = a.area + b.area - intersection
        return if (union == 0) 0f else intersection.toFloat() / union.toFloat()
    }

    private fun estimateBackgroundLuma(sampled: SampledImage): Int {
        val corners = intArrayOf(
            sampled.luma[0],
            sampled.luma[sampled.width - 1],
            sampled.luma[sampled.width * (sampled.height - 1)],
            sampled.luma[sampled.width * sampled.height - 1]
        )
        return corners.average().toInt()
    }

    private fun computeOtsuThreshold(histogram: IntArray, total: Int): Int {
        var sum = 0.0
        for (i in histogram.indices) {
            sum += i * histogram[i]
        }
        var sumB = 0.0
        var weightBackground = 0.0
        var maxVariance = 0.0
        var threshold = 127
        for (i in histogram.indices) {
            weightBackground += histogram[i]
            if (weightBackground == 0.0) continue
            val weightForeground = total - weightBackground
            if (weightForeground == 0.0) break
            sumB += i * histogram[i]
            val meanBackground = sumB / weightBackground
            val meanForeground = (sum - sumB) / weightForeground
            val varianceBetween = weightBackground * weightForeground * (meanBackground - meanForeground) * (meanBackground - meanForeground)
            if (varianceBetween > maxVariance) {
                maxVariance = varianceBetween
                threshold = i
            }
        }
        return threshold.coerceIn(32, 223)
    }

    private fun colorLuma(color: Int): Int {
        val r = color shr 16 and 0xFF
        val g = color shr 8 and 0xFF
        val b = color and 0xFF
        return ((0.2126 * r) + (0.7152 * g) + (0.0722 * b)).toInt()
    }
}

class BitmapPanelImage(private val bitmap: Bitmap) : PanelImage {
    override val width: Int get() = bitmap.width
    override val height: Int get() = bitmap.height
    override fun getPixel(x: Int, y: Int): Int = bitmap.getPixel(x, y)
}


