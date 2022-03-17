package org.koitharu.kotatsu.reader.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.databinding.ActivitySettingsSimpleBinding
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.settings.*

class SimpleSettingsActivity : BaseActivity<ActivitySettingsSimpleBinding>() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySettingsSimpleBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportFragmentManager.commit {
			replace(
				R.id.container,
				when (intent?.action) {
					Intent.ACTION_MANAGE_NETWORK_USAGE -> NetworkSettingsFragment()
					ACTION_READER -> ReaderSettingsFragment()
					ACTION_SUGGESTIONS -> SuggestionsSettingsFragment()
					ACTION_SOURCE -> SourceSettingsFragment.newInstance(
						intent.getSerializableExtra(EXTRA_SOURCE) as? MangaSource ?: MangaSource.LOCAL
					)
					else -> MainSettingsFragment()
				}
			)
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		with(binding.toolbar) {
			updatePadding(
				left = insets.left,
				right = insets.right
			)
			updateLayoutParams<ViewGroup.MarginLayoutParams> {
				topMargin = insets.top
			}
		}
	}

	companion object {

		private const val ACTION_READER =
			"${BuildConfig.APPLICATION_ID}.action.MANAGE_READER_SETTINGS"
		private const val ACTION_SUGGESTIONS =
			"${BuildConfig.APPLICATION_ID}.action.MANAGE_SUGGESTIONS"
		private const val ACTION_SOURCE =
			"${BuildConfig.APPLICATION_ID}.action.MANAGE_SOURCE_SETTINGS"
		private const val EXTRA_SOURCE = "source"

		fun newReaderSettingsIntent(context: Context) =
			Intent(context, SimpleSettingsActivity::class.java)
				.setAction(ACTION_READER)

		fun newSuggestionsSettingsIntent(context: Context) =
			Intent(context, SimpleSettingsActivity::class.java)
				.setAction(ACTION_SUGGESTIONS)

		fun newSourceSettingsIntent(context: Context, source: MangaSource) =
			Intent(context, SimpleSettingsActivity::class.java)
				.setAction(ACTION_SOURCE)
				.putExtra(EXTRA_SOURCE, source)
	}
}