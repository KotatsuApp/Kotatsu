package org.koitharu.kotatsu.reader.domain.panel

data class PanelPreferences(
    val disableFrame: Boolean = false,
    val inlineFrames: Boolean = false,
    val scanType: PanelScanMode = PanelScanMode.REGULAR,
    val readingOrder: PanelReadingOrder = PanelReadingOrder.MANGA,
    val autoSwitchScan: Boolean = true,
    val fitToWidth: Boolean = false,
    val panBound: Boolean = true,
    val borderOpacity: Float = 0.5f,
)

enum class PanelScanMode {
    REGULAR,
    IRREGULAR,
    FOUR_QUADRANTS,
    WEBTOON,
}

enum class PanelReadingOrder {
    STANDARD,
    MANGA,
    KOMA4;

    fun toPanelFlow(): PanelFlow = when (this) {
        STANDARD -> PanelFlow.LeftToRight,
        MANGA -> PanelFlow.RightToLeft,
        KOMA4 -> PanelFlow.TopToBottom,
    }
}
