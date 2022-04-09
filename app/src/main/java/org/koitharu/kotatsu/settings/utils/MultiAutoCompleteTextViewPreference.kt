package org.koitharu.kotatsu.settings.utils

import android.content.Context
import android.util.AttributeSet
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Filter
import android.widget.MultiAutoCompleteTextView
import androidx.annotation.AttrRes
import androidx.annotation.MainThread
import androidx.annotation.StyleRes
import androidx.annotation.WorkerThread
import androidx.preference.EditTextPreference
import kotlinx.coroutines.runBlocking
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.util.replaceWith

class MultiAutoCompleteTextViewPreference @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = R.attr.multiAutoCompleteTextViewPreferenceStyle,
	@StyleRes defStyleRes: Int = R.style.Preference_MultiAutoCompleteTextView,
) : EditTextPreference(context, attrs, defStyleAttr, defStyleRes) {

	private val autoCompleteBindListener = AutoCompleteBindListener()

	var autoCompleteProvider: AutoCompleteProvider? = null

	init {
		super.setOnBindEditTextListener(autoCompleteBindListener)
	}

	override fun setOnBindEditTextListener(onBindEditTextListener: OnBindEditTextListener?) {
		autoCompleteBindListener.delegate = onBindEditTextListener
	}

	private inner class AutoCompleteBindListener : OnBindEditTextListener {

		var delegate: OnBindEditTextListener? = null

		override fun onBindEditText(editText: EditText) {
			delegate?.onBindEditText(editText)
			if (editText !is MultiAutoCompleteTextView) {
				return
			}
			editText.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
			editText.setAdapter(
				autoCompleteProvider?.let {
					CompletionAdapter(editText.context, it, ArrayList())
				}
			)
			editText.threshold = 1
		}
	}

	interface AutoCompleteProvider {

		suspend fun getSuggestions(query: String): List<String>
	}

	class SimpleSummaryProvider(
		private val emptySummary: CharSequence?,
	) : SummaryProvider<MultiAutoCompleteTextViewPreference> {

		override fun provideSummary(preference: MultiAutoCompleteTextViewPreference): CharSequence? {
			return if (preference.text.isNullOrEmpty()) {
				emptySummary
			} else {
				preference.text?.trimEnd(' ', ',')
			}
		}
	}

	private class CompletionAdapter(
		context: Context,
		private val completionProvider: AutoCompleteProvider,
		private val dataset: MutableList<String>,
	) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, dataset) {

		override fun getFilter(): Filter {
			return CompletionFilter(this, completionProvider)
		}

		fun publishResults(results: List<String>) {
			dataset.replaceWith(results)
			notifyDataSetChanged()
		}
	}

	private class CompletionFilter(
		private val adapter: CompletionAdapter,
		private val provider: AutoCompleteProvider,
	) : Filter() {

		@WorkerThread
		override fun performFiltering(constraint: CharSequence?): FilterResults {
			val query = constraint?.toString().orEmpty()
			val suggestions = runBlocking { provider.getSuggestions(query) }
			return CompletionResults(suggestions)
		}

		@MainThread
		override fun publishResults(constraint: CharSequence?, results: FilterResults) {
			val completions = (results as CompletionResults).completions
			adapter.publishResults(completions)
		}

		private class CompletionResults(
			val completions: List<String>,
		) : FilterResults() {

			init {
				values = completions
				count = completions.size
			}
		}
	}
}