package org.koitharu.kotatsu.search.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.parameter.parametersOf
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.core.model.parcelable.ParcelableMangaTags
import org.koitharu.kotatsu.databinding.ActivityContainerBinding
import org.koitharu.kotatsu.local.ui.LocalListFragment
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.remotelist.ui.RemoteListFragment
import org.koitharu.kotatsu.remotelist.ui.RemoteListViewModel

class MangaListActivity : BaseActivity<ActivityContainerBinding>() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityContainerBinding.inflate(layoutInflater))
		val tags = intent.getParcelableExtra<ParcelableMangaTags>(EXTRA_TAGS)?.tags
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val fm = supportFragmentManager
		if (fm.findFragmentById(R.id.container) == null) {
			val source = intent.getSerializableExtra(EXTRA_SOURCE) as? MangaSource ?: tags?.firstOrNull()?.source
			if (source == null) {
				finishAfterTransition()
				return
			}
			fm.commit {
				val fragment = if (source == MangaSource.LOCAL) {
					LocalListFragment.newInstance()
				} else {
					RemoteListFragment.newInstance(source)
				}
				replace(R.id.container, fragment)
				if (!tags.isNullOrEmpty()) {
					runOnCommit(ApplyFilterRunnable(fragment, tags))
				}
			}
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		with(binding.toolbar) {
			updatePadding(
				left = insets.left,
				right = insets.right
			)
		}
	}

	private class ApplyFilterRunnable(
		private val fragment: Fragment,
		private val tags: Set<MangaTag>,
	) : Runnable {

		override fun run() {
			val viewModel = fragment.getViewModel<RemoteListViewModel> {
				parametersOf(tags.first().source)
			}
			viewModel.applyFilter(tags)
		}
	}

	companion object {

		private const val EXTRA_TAGS = "tags"
		private const val EXTRA_SOURCE = "source"

		fun newIntent(context: Context, tags: Set<MangaTag>) = Intent(context, MangaListActivity::class.java)
			.putExtra(EXTRA_TAGS, ParcelableMangaTags(tags))

		fun newIntent(context: Context, source: MangaSource) = Intent(context, MangaListActivity::class.java)
			.putExtra(EXTRA_SOURCE, source)
	}
}