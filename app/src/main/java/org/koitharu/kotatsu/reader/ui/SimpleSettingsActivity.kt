package org.koitharu.kotatsu.reader.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.databinding.ActivitySettingsSimpleBinding
import org.koitharu.kotatsu.settings.MainSettingsFragment
import org.koitharu.kotatsu.settings.NetworkSettingsFragment
import org.koitharu.kotatsu.settings.ReaderSettingsFragment

class SimpleSettingsActivity : BaseActivity<ActivitySettingsSimpleBinding>() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySettingsSimpleBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportFragmentManager.commit {
			replace(
				R.id.container, when (intent?.action) {
					Intent.ACTION_MANAGE_NETWORK_USAGE -> NetworkSettingsFragment()
					ACTION_READER -> ReaderSettingsFragment()
					else -> MainSettingsFragment()
				}
			)
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.toolbar.updatePadding(
			top = insets.top,
			left = insets.left,
			right = insets.right
		)
	}

	companion object {

		private const val ACTION_READER =
			"${BuildConfig.APPLICATION_ID}.action.MANAGE_READER_SETTINGS"

		fun newReaderSettingsIntent(context: Context) =
			Intent(context, SimpleSettingsActivity::class.java)
				.setAction(ACTION_READER)
	}
}