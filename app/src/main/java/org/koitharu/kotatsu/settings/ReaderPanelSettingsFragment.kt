package org.koitharu.kotatsu.settings

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.settings.utils.SliderPreference
import org.koitharu.kotatsu.reader.domain.panel.PanelScanMode
import org.koitharu.kotatsu.reader.domain.panel.PanelReadingOrder

@AndroidEntryPoint
class ReaderPanelSettingsFragment : BasePreferenceFragment() {

    @Inject
    lateinit var settings: AppSettings

    private lateinit var settings: AppSettings

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(org.koitharu.kotatsu.R.xml.reader_panel_view_settings, rootKey)

        setupPreferences()
        updateEnabledState()
    }

    private fun setupPreferences() {
        findPreference<SwitchPreferenceCompat>("pref_panel_disable_frame")?.setOnPreferenceChangeListener { _, newValue ->
            settings.isPanelDisableFrame = newValue as Boolean
            updateFramePreferences(newValue)
            true
        }

        findPreference<ListPreference>("pref_panel_scan_type")?.let { pref ->
            pref.setOnPreferenceChangeListener { _, newValue ->
                settings.panelScanType = PanelScanMode.valueOf(newValue.toString())
                updateEnabledState()
                true
            }
        }

        findPreference<ListPreference>("pref_panel_reading_order")?.let { pref ->
            pref.setOnPreferenceChangeListener { _, newValue ->
                settings.panelReadingOrder = PanelReadingOrder.valueOf(newValue.toString())
                true
            }
        }

        findPreference<SwitchPreferenceCompat>("pref_panel_auto_switch_scan")?.let { pref ->
            pref.setOnPreferenceChangeListener { _, newValue ->
                settings.isPanelAutoSwitchScan = newValue as Boolean
                true
            }
        }

        findPreference<SwitchPreferenceCompat>("pref_panel_fit_to_width")?.let { pref ->
            pref.setOnPreferenceChangeListener { _, newValue ->
                settings.isPanelFitToWidth = newValue as Boolean
                true
            }
        }

        findPreference<SwitchPreferenceCompat>("pref_panel_pan_bound")?.let { pref ->
            pref.setOnPreferenceChangeListener { _, newValue ->
                settings.isPanelPanBound = newValue as Boolean
                true
            }
        }

        findPreference<SliderPreference>("pref_panel_border_opacity")?.let { pref ->
            pref.setOnPreferenceChangeListener { _, newValue ->
                settings.panelBorderOpacity = newValue as Float
                true
            }
        }
    }

    private fun updateFramePreferences(isDisabled: Boolean) {
        findPreference<Preference>("pref_panel_inline_frames")?.isEnabled = !isDisabled
        updateEnabledState()
    }

    private fun updateEnabledState() {
        val isDisabled = settings.isPanelDisableFrame
        findPreference<Preference>("pref_panel_inline_frames")?.isEnabled = !isDisabled
        findPreference<Preference>("panel_group_scan_type")?.isEnabled = !isDisabled
        findPreference<Preference>("panel_group_reading_order")?.isEnabled = !isDisabled
        findPreference<Preference>("panel_group_enhancements")?.isEnabled = !isDisabled
    }
}
