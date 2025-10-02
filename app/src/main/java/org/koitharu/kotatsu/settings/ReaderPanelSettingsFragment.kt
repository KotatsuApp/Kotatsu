package org.koitharu.kotatsu.settings

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.util.ext.setDefaultValueCompat
import org.koitharu.kotatsu.parsers.util.names
import org.koitharu.kotatsu.reader.domain.panel.PanelPreferences
import org.koitharu.kotatsu.reader.domain.panel.PanelReadingOrder
import org.koitharu.kotatsu.reader.domain.panel.PanelScanMode
import org.koitharu.kotatsu.reader.domain.panel.PanelSettingsRepository
import org.koitharu.kotatsu.settings.utils.PercentSummaryProvider
import org.koitharu.kotatsu.settings.utils.SliderPreference

@AndroidEntryPoint
class ReaderPanelSettingsFragment : BasePreferenceFragment(R.string.panel_settings_category) {

    @Inject
    lateinit var panelSettings: PanelSettingsRepository

    private var isPanelModeEnabled = false
    private var latestPreferences: PanelPreferences? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.reader_panel_view_settings, rootKey)
        setupPreferences()
        ensureAutoSwitchDefault()

        val initialPanelEnabled = panelSettings.isPanelViewEnabled()
        val initialPreferences = panelSettings.getPreferences()
        latestPreferences = initialPreferences
        updatePanelModeVisibility(initialPanelEnabled)
        applyPreferenceState(initialPreferences)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    panelSettings.panelViewEnabledFlow.collect { enabled ->
                        updatePanelModeVisibility(enabled)
                        latestPreferences?.let { applyPreferenceState(it) }
                    }
                }
                launch {
                    panelSettings.preferencesFlow.collect { prefs ->
                        applyPreferenceState(prefs)
                    }
                }
            }
        }
    }

    private fun setupPreferences() {
        findPreference<Preference>(PREF_PANEL_DISABLED_INFO)?.icon = getWarningIcon()

        findPreference<SwitchPreferenceCompat>(PREF_DISABLE_FRAME)?.setOnPreferenceChangeListener { _, newValue ->
            val disable = newValue as? Boolean ?: return@setOnPreferenceChangeListener false
            panelSettings.updateDisableFrame(disable)
            true
        }

        findPreference<SwitchPreferenceCompat>(PREF_INLINE_FRAMES)?.setOnPreferenceChangeListener { _, newValue ->
            panelSettings.updateInlineFrames(newValue as Boolean)
            true
        }

        findPreference<ListPreference>(PREF_SCAN_TYPE)?.apply {
            entryValues = PanelScanMode.entries.names()
            setDefaultValueCompat(PanelScanMode.REGULAR.name)
            setOnPreferenceChangeListener { _, newValue ->
                panelSettings.updateScanType(PanelScanMode.valueOf(newValue.toString()))
                true
            }
        }

        findPreference<ListPreference>(PREF_READING_ORDER)?.apply {
            entryValues = PanelReadingOrder.entries.names()
            setDefaultValueCompat(PanelReadingOrder.MANGA.name)
            setOnPreferenceChangeListener { _, newValue ->
                val order = PanelReadingOrder.valueOf(newValue.toString())
                panelSettings.updateReadingOrder(order)
                if (order == PanelReadingOrder.MANGA) {
                    panelSettings.updateAutoSwitchScan(true)
                }
                true
            }
        }

        findPreference<SwitchPreferenceCompat>(PREF_AUTO_SWITCH)?.apply {
            summary = getString(R.string.panel_auto_switch_irregular_summary)
            setOnPreferenceChangeListener { _, newValue ->
                panelSettings.updateAutoSwitchScan(newValue as Boolean)
                true
            }
        }

        findPreference<SwitchPreferenceCompat>(PREF_FIT_TO_WIDTH)?.setOnPreferenceChangeListener { _, newValue ->
            panelSettings.updateFitToWidth(newValue as Boolean)
            true
        }

        findPreference<SwitchPreferenceCompat>(PREF_PAN_BOUND)?.setOnPreferenceChangeListener { _, newValue ->
            panelSettings.updatePanBound(newValue as Boolean)
            true
        }

        findPreference<SliderPreference>(PREF_BORDER_OPACITY)?.apply {
            summaryProvider = PercentSummaryProvider()
            setOnPreferenceChangeListener { _, newValue ->
                val percent = (newValue as Int).coerceIn(0, 100)
                panelSettings.updateBorderOpacity(percent / 100f)
                true
            }
        }
    }

    private fun ensureAutoSwitchDefault() {
        val prefs = preferenceManager.sharedPreferences ?: return
        if (!prefs.contains(PREF_AUTO_SWITCH)) {
            val defaultValue = panelSettings.getPreferences().readingOrder == PanelReadingOrder.MANGA
            prefs.edit().putBoolean(PREF_AUTO_SWITCH, defaultValue).apply()
            findPreference<SwitchPreferenceCompat>(PREF_AUTO_SWITCH)?.isChecked = defaultValue
        }
    }

    private fun updatePanelModeVisibility(isEnabled: Boolean) {
        isPanelModeEnabled = isEnabled
        findPreference<Preference>(PREF_PANEL_DISABLED_INFO)?.apply {
            icon = icon ?: getWarningIcon()
            isVisible = !isEnabled
        }
        PANEL_SECTION_KEYS.forEach { key ->
            findPreference<Preference>(key)?.isVisible = isEnabled
        }
    }

    private fun applyPreferenceState(preferences: PanelPreferences) {
        latestPreferences = preferences

        val detectionEnabled = isPanelModeEnabled && !preferences.disableFrame
        findPreference<Preference>(PREF_INLINE_FRAMES)?.isEnabled = detectionEnabled

        ADVANCED_SECTION_KEYS.forEach { key ->
            findPreference<Preference>(key)?.isEnabled = detectionEnabled
        }

        updateAutoSwitchAvailability(preferences)
    }

    private fun updateAutoSwitchAvailability(preferences: PanelPreferences?) {
        val autoSwitchPref = findPreference<SwitchPreferenceCompat>(PREF_AUTO_SWITCH) ?: return
        if (preferences == null) {
            autoSwitchPref.isEnabled = false
            return
        }
        val canConfigure = isPanelModeEnabled && !preferences.disableFrame && preferences.scanType == PanelScanMode.REGULAR
        autoSwitchPref.isEnabled = canConfigure
    }

    companion object {
        private const val PREF_DISABLE_FRAME = "pref_panel_disable_frame"
        private const val PREF_INLINE_FRAMES = "pref_panel_inline_frames"
        private const val PREF_SCAN_TYPE = "pref_panel_scan_type"
        private const val PREF_READING_ORDER = "pref_panel_reading_order"
        private const val PREF_AUTO_SWITCH = "pref_panel_auto_switch_scan"
        private const val PREF_FIT_TO_WIDTH = "pref_panel_fit_to_width"
        private const val PREF_PAN_BOUND = "pref_panel_pan_bound"
        private const val PREF_BORDER_OPACITY = "pref_panel_border_opacity"
        private const val PREF_PANEL_DISABLED_INFO = "panel_settings_disabled_info"

        private val PANEL_SECTION_KEYS = arrayOf(
            "panel_group_frame_detection",
            "panel_group_scan_type",
            "panel_group_reading_order",
            "panel_group_enhancements",
        )

        private val ADVANCED_SECTION_KEYS = arrayOf(
            "panel_group_scan_type",
            "panel_group_reading_order",
            "panel_group_enhancements",
        )
    }
}
