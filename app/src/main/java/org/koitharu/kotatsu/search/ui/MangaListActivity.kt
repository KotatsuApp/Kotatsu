package org.koitharu.kotatsu.search.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.core.model.parcelable.ParcelableMangaTags
import org.koitharu.kotatsu.databinding.ActivityContainerBinding
import org.koitharu.kotatsu.local.ui.LocalListFragment
import org.koitharu.kotatsu.main.ui.AppBarOwner
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.remotelist.ui.RemoteListFragment

@AndroidEntryPoint
class MangaListActivity :
	BaseActivity<ActivityContainerBinding>(),
	AppBarOwner {

	override val appBar: AppBarLayout
		get() = binding.appbar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityContainerBinding.inflate(layoutInflater))
		val tags = intent.getParcelableExtra<ParcelableMangaTags>(EXTRA_TAGS)?.tags
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val source = intent.getSerializableExtra(EXTRA_SOURCE) as? MangaSource ?: tags?.firstOrNull()?.source
		if (source == null) {
			finishAfterTransition()
			return
		}
		title = if (source == MangaSource.LOCAL) getString(R.string.local_storage) else source.title
		val fm = supportFragmentManager
		if (fm.findFragmentById(R.id.container) == null) {
			fm.commit {
				val fragment = if (source == MangaSource.LOCAL) {
					LocalListFragment.newInstance()
				} else {
					RemoteListFragment.newInstance(source)
				}
				replace(R.id.container, fragment)
				if (!tags.isNullOrEmpty() && fragment is RemoteListFragment) {
					runOnCommit(ApplyFilterRunnable(fragment, tags))
				}
			}
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
	}

	private class ApplyFilterRunnable(
		private val fragment: RemoteListFragment,
		private val tags: Set<MangaTag>,
	) : Runnable {

		override fun run() {
			fragment.viewModel.applyFilter(tags)
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
