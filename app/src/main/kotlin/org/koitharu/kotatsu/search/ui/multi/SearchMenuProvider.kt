package org.koitharu.kotatsu.search.ui.multi

import android.os.Build
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.search.domain.SearchKind

class SearchMenuProvider(
	private val activity: SearchActivity,
	private val viewModel: SearchViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_search_kind, menu)
	}

	override fun onPrepareMenu(menu: Menu) {
		super.onPrepareMenu(menu)
		menu.findItem(
			when (viewModel.kind) {
				SearchKind.SIMPLE -> R.id.action_kind_simple
				SearchKind.TITLE -> R.id.action_kind_title
				SearchKind.AUTHOR -> R.id.action_kind_author
				SearchKind.TAG -> R.id.action_kind_tag
			},
		)?.isChecked = true
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		when (menuItem.itemId) {
			R.id.action_filter_pinned_only -> {
				menuItem.isChecked = !menuItem.isChecked
				viewModel.setPinnedOnly(menuItem.isChecked)
				return true
			}

			R.id.action_filter_hide_empty -> {
				menuItem.isChecked = !menuItem.isChecked
				viewModel.setHideEmpty(menuItem.isChecked)
				return true
			}
		}

		val newKind = when (menuItem.itemId) {
			R.id.action_kind_simple -> SearchKind.SIMPLE
			R.id.action_kind_title -> SearchKind.TITLE
			R.id.action_kind_author -> SearchKind.AUTHOR
			R.id.action_kind_tag -> SearchKind.TAG
			else -> return false
		}
		if (newKind != viewModel.kind) {
			activity.router.openSearch(
				query = viewModel.query,
				kind = newKind,
			)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out, 0)
			} else {
				activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
			}
			activity.finishAfterTransition()
		}
		return true
	}
}
