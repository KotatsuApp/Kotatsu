package org.koitharu.kotatsu.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.settings.utils.MultiAutoCompleteTextViewPreference
import org.koitharu.kotatsu.settings.utils.TagsAutoCompleteProvider
import org.koitharu.kotatsu.suggestions.domain.SuggestionRepository
import org.koitharu.kotatsu.suggestions.ui.SuggestionsWorker
import javax.inject.Inject

@AndroidEntryPoint
class SuggestionsSettingsFragment :
	BasePreferenceFragment(R.string.suggestions),
	SharedPreferences.OnSharedPreferenceChangeListener {

	@Inject
	lateinit var repository: SuggestionRepository

	@Inject
	lateinit var tagsCompletionProvider: TagsAutoCompleteProvider

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		settings.subscribe(this)
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_suggestions)

		findPreference<MultiAutoCompleteTextViewPreference>(AppSettings.KEY_SUGGESTIONS_EXCLUDE_TAGS)?.run {
			autoCompleteProvider = tagsCompletionProvider
			summaryProvider = MultiAutoCompleteTextViewPreference.SimpleSummaryProvider(summary)
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		settings.unsubscribe(this)
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		if (key == AppSettings.KEY_SUGGESTIONS && settings.isSuggestionsEnabled) {
			onSuggestionsEnabled()
		}
	}

	private fun onSuggestionsEnabled() {
		lifecycleScope.launch {
			if (repository.isEmpty()) {
				SuggestionsWorker.startNow(context ?: return@launch)
			}
		}
	}
}
