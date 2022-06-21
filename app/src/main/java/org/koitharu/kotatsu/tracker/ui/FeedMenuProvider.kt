package org.koitharu.kotatsu.tracker.ui

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.tracker.work.TrackWorker

class FeedMenuProvider(
	private val snackbarHost: View,
	private val viewModel: FeedViewModel,
) : MenuProvider {

	private val context: Context
		get() = snackbarHost.context

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_feed, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
		R.id.action_update -> {
			TrackWorker.startNow(context)
			Snackbar.make(
				snackbarHost,
				R.string.feed_will_update_soon,
				Snackbar.LENGTH_LONG,
			).show()
			true
		}
		R.id.action_clear_feed -> {
			MaterialAlertDialogBuilder(context)
				.setTitle(R.string.clear_updates_feed)
				.setMessage(R.string.text_clear_updates_feed_prompt)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(R.string.clear) { _, _ ->
					viewModel.clearFeed()
				}.show()
			true
		}
		else -> false
	}
}