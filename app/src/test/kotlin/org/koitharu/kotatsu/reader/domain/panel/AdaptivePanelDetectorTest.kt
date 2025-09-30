package org.koitharu.kotatsu.reader.domain.panel

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptivePanelDetectorTest {

    private val detector = AdaptivePanelDetector()

    @Test
    fun singlePanelIsDetected() = runTest {
        val image = arrayImage(width = 6, height = 6) { x, y ->
            if (x in 1..4 && y in 1..4) {
                BLACK
            } else {
                WHITE
            }
        }
        val result = detector.detect(PanelDetectionRequest(image = image))

        assertEquals(1, result.primary.panelCount)
        assertEquals(PanelLayoutType.Detected, result.primary.layoutType)
        assertTrue(result.issues.isEmpty())

        val panel = result.primary.panels.single()
        assertEquals(RectInt(1, 1, 5, 5), panel.bounds)
        assertEquals(PanelFlow.LeftToRight, result.primary.flow)
    }

    @Test
    fun verticalSplitProducesTwoPanels() = runTest {
        val image = arrayImage(width = 9, height = 6) { x, _ ->
            when (x) {
                in 1..3 -> BLACK
                in 5..7 -> BLACK
                else -> WHITE
            }
        }
        val result = detector.detect(PanelDetectionRequest(image = image))

        assertEquals(2, result.primary.panelCount)
        assertEquals(PanelLayoutType.Detected, result.primary.layoutType)
        val (left, right) = result.primary.panels
        assertTrue(left.bounds.left < right.bounds.left)
        assertTrue(result.alternatives.any { it.layoutType == PanelLayoutType.FallbackFullPage })
    }

    @Test
    fun rightToLeftFlowReordersPanels() = runTest {
        val image = arrayImage(width = 9, height = 6) { x, _ ->
            when (x) {
                in 1..3 -> BLACK
                in 5..7 -> BLACK
                else -> WHITE
            }
        }
        val result = detector.detect(
            PanelDetectionRequest(
                image = image,
                preferredFlow = PanelFlow.RightToLeft
            )
        )
        assertEquals(2, result.primary.panelCount)
        val (first, second) = result.primary.panels
        assertTrue(first.bounds.left > second.bounds.left)
    }

    @Test
    fun emptyPageFallsBackToFullPage() = runTest {
        val image = arrayImage(width = 8, height = 8) { _, _ -> WHITE }
        val result = detector.detect(PanelDetectionRequest(image = image))

        assertEquals(PanelLayoutType.FallbackFullPage, result.primary.layoutType)
        assertEquals(1, result.primary.panelCount)
        assertFalse(result.issues.isEmpty())
    }

    @Test
    fun ignoresSmallNoiseWhenThresholdRaised() = runTest {
        val image = arrayImage(width = 10, height = 10) { x, y ->
            when {
                x == 0 && y == 0 -> BLACK
                x in 2..8 && y in 1..8 -> BLACK
                else -> WHITE
            }
        }
        val result = detector.detect(
            PanelDetectionRequest(
                image = image,
                minPanelAreaRatio = 0.05f // exclude the 1px noise panel
            )
        )
        assertEquals(1, result.primary.panelCount)
        assertEquals(RectInt(2, 1, 9, 9), result.primary.panels.single().bounds)
    }

    private fun arrayImage(width: Int, height: Int, builder: (Int, Int) -> Int): PanelImage {
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            builder(x, y)
        }
        return object : PanelImage {
            override val width: Int = width
            override val height: Int = height
            override fun getPixel(x: Int, y: Int): Int = pixels[y * width + x]
        }
    }

    private companion object {
        const val WHITE: Int = 0xFFFFFFFF.toInt()
        const val BLACK: Int = 0xFF000000.toInt()
    }
}

