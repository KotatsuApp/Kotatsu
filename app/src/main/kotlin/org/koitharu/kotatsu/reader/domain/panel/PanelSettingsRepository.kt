package org.koitharu.kotatsu.reader.domain.panel

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.koitharu.kotatsu.core.prefs.AppSettings

@Singleton
class PanelSettingsRepository @Inject constructor(
    private val settings: AppSettings,
) {

    private val observedKeys = setOf(
        AppSettings.KEY_READER_PANEL_MODE,
        AppSettings.KEY_PANEL_DISABLE_FRAME,
        AppSettings.KEY_PANEL_INLINE_FRAMES,
        AppSettings.KEY_PANEL_SCAN_TYPE,
        AppSettings.KEY_PANEL_READING_ORDER,
        AppSettings.KEY_PANEL_AUTO_SWITCH_SCAN,
        AppSettings.KEY_PANEL_FIT_TO_WIDTH,
        AppSettings.KEY_PANEL_PAN_BOUND,
        AppSettings.KEY_PANEL_BORDER_OPACITY,
    )

    val preferencesFlow: Flow<PanelPreferences> = settings.observeChanges()
        .filter { key -> key == null || key in observedKeys }
        .onStart { emit(null) }
        .map { getPreferences() }

    val panelViewEnabledFlow: Flow<Boolean> = settings.observe(AppSettings.KEY_READER_PANEL_MODE)
        .onStart { emit(null) }
        .map { settings.isReaderPanelModeEnabled }

    fun getPreferences(): PanelPreferences = PanelPreferences(
        disableFrame = settings.isPanelDisableFrame,
        inlineFrames = settings.isPanelInlineFrames,
        scanType = settings.panelScanType,
        readingOrder = settings.panelReadingOrder,
        autoSwitchScan = settings.isPanelAutoSwitchScan,
        fitToWidth = settings.isPanelFitToWidth,
        panBound = settings.isPanelPanBound,
        borderOpacity = settings.panelBorderOpacity,
    )

    fun isPanelViewEnabled(): Boolean = settings.isReaderPanelModeEnabled

    fun updatePanelMode(enabled: Boolean) {
        settings.isReaderPanelModeEnabled = enabled
    }

    fun updateDisableFrame(disable: Boolean) {
        settings.isPanelDisableFrame = disable
    }

    fun updateInlineFrames(enabled: Boolean) {
        settings.isPanelInlineFrames = enabled
    }

    fun updateScanType(mode: PanelScanMode) {
        settings.panelScanType = mode
    }

    fun updateReadingOrder(order: PanelReadingOrder) {
        settings.panelReadingOrder = order
    }

    fun updateAutoSwitchScan(enabled: Boolean) {
        settings.isPanelAutoSwitchScan = enabled
    }

    fun updateFitToWidth(enabled: Boolean) {
        settings.isPanelFitToWidth = enabled
    }

    fun updatePanBound(enabled: Boolean) {
        settings.isPanelPanBound = enabled
    }

    fun updateBorderOpacity(opacity: Float) {
        settings.panelBorderOpacity = opacity
    }
}
