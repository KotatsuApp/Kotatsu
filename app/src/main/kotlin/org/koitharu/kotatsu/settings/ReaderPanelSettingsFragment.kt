package org.koitharu.kotatsu.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.util.ext.setDefaultValueCompat
import org.koitharu.kotatsu.parsers.util.names
import org.koitharu.kotatsu.reader.domain.panel.PanelReadingOrder
import org.koitharu.kotatsu.reader.domain.panel.PanelScanMode
import org.koitharu.kotatsu.settings.utils.PercentSummaryProvider
import org.koitharu.kotatsu.settings.utils.SliderPreference

@AndroidEntryPoint
class ReaderPanelSettingsFragment :
    BasePreferenceFragment(R.string.panel_settings_category),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val conditionalKeys = listOf(
        "panel_group_frame_detection",
        "panel_group_scan_type",
        "panel_group_reading_order",
        "panel_group_enhancements",
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.reader_panel_view_settings)
        findPreference<Preference>("panel_settings_disabled_info")?.icon = getWarningIcon()

        configureScanTypePreference()
        configureReadingOrderPreference()
        configureBorderOpacityPreference()
        ensureAutoSwitchDefault()

        updatePanelSettingsVisibility()
        updateAutoSwitchAvailability()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings.subscribe(this)
    }

    override fun onDestroyView() {
        settings.unsubscribe(this)
        super.onDestroyView()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            AppSettings.KEY_READER_PANEL_MODE -> updatePanelSettingsVisibility()
            AppSettings.KEY_PANEL_SCAN_TYPE -> updateAutoSwitchAvailability()
            AppSettings.KEY_PANEL_READING_ORDER -> onReadingOrderChanged()
        }
    }

    private fun configureScanTypePreference() {
        findPreference<ListPreference>(AppSettings.KEY_PANEL_SCAN_TYPE)?.run {
            entryValues = PanelScanMode.entries.names()
            setDefaultValueCompat(PanelScanMode.REGULAR.name)
        }
    }

    private fun configureReadingOrderPreference() {
        findPreference<ListPreference>(AppSettings.KEY_PANEL_READING_ORDER)?.run {
            entryValues = PanelReadingOrder.entries.names()
            setDefaultValueCompat(PanelReadingOrder.MANGA.name)
        }
        findPreference<SwitchPreferenceCompat>(AppSettings.KEY_PANEL_AUTO_SWITCH_SCAN)?.apply {
            summary = getString(R.string.panel_auto_switch_irregular_summary)
        }
    }

    private fun configureBorderOpacityPreference() {
        findPreference<SliderPreference>(AppSettings.KEY_PANEL_BORDER_OPACITY)?.summaryProvider = PercentSummaryProvider()
    }

    private fun ensureAutoSwitchDefault() {
        val prefs = preferenceManager.sharedPreferences ?: return
        if (!prefs.contains(AppSettings.KEY_PANEL_AUTO_SWITCH_SCAN)) {
            val defaultValue = settings.panelReadingOrder == PanelReadingOrder.MANGA
            prefs.edit().putBoolean(AppSettings.KEY_PANEL_AUTO_SWITCH_SCAN, defaultValue).apply()
            findPreference<SwitchPreferenceCompat>(AppSettings.KEY_PANEL_AUTO_SWITCH_SCAN)?.isChecked = defaultValue
        }
    }

    private fun onReadingOrderChanged() {
        if (settings.panelReadingOrder == PanelReadingOrder.MANGA) {
            findPreference<SwitchPreferenceCompat>(AppSettings.KEY_PANEL_AUTO_SWITCH_SCAN)?.let { pref ->
                if (!pref.isChecked) {
                    pref.isChecked = true
                }
            }
        }
        updateAutoSwitchAvailability()
    }

    private fun updateAutoSwitchAvailability() {
        val isRegular = settings.panelScanType == PanelScanMode.REGULAR
        findPreference<SwitchPreferenceCompat>(AppSettings.KEY_PANEL_AUTO_SWITCH_SCAN)?.apply {
            isEnabled = isRegular
        }
    }

    private fun updatePanelSettingsVisibility() {
        val isPanelEnabled = settings.isReaderPanelModeEnabled
        conditionalKeys.forEach { key ->
            findPreference<Preference>(key)?.isVisible = isPanelEnabled
        }
        findPreference<Preference>("panel_settings_disabled_info")?.isVisible = !isPanelEnabled
    }
}