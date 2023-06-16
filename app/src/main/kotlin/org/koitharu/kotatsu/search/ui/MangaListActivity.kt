package org.koitharu.kotatsu.search.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.parcelable.ParcelableMangaTags
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.model.titleRes
import org.koitharu.kotatsu.core.util.ext.getParcelableExtraCompat
import org.koitharu.kotatsu.core.util.ext.getSerializableExtraCompat
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.setTextAndVisible
import org.koitharu.kotatsu.databinding.ActivityMangaListBinding
import org.koitharu.kotatsu.filter.ui.FilterHeaderFragment
import org.koitharu.kotatsu.filter.ui.FilterOwner
import org.koitharu.kotatsu.filter.ui.FilterSheetFragment
import org.koitharu.kotatsu.local.ui.LocalListFragment
import org.koitharu.kotatsu.main.ui.owners.AppBarOwner
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.remotelist.ui.RemoteListFragment

@AndroidEntryPoint
class MangaListActivity :
	BaseActivity<ActivityMangaListBinding>(),
	AppBarOwner, View.OnClickListener {

	override val appBar: AppBarLayout
		get() = viewBinding.appbar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityMangaListBinding.inflate(layoutInflater))
		val tags = intent.getParcelableExtraCompat<ParcelableMangaTags>(EXTRA_TAGS)?.tags
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val source = intent.getSerializableExtraCompat(EXTRA_SOURCE) ?: tags?.firstOrNull()?.source
		if (source == null) {
			finishAfterTransition()
			return
		}
		viewBinding.chipSort?.setOnClickListener(this)
		title = if (source == MangaSource.LOCAL) getString(R.string.local_storage) else source.title
		initList(source, tags)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		viewBinding.cardFilter?.updateLayoutParams<MarginLayoutParams> {
			bottomMargin = marginStart + insets.bottom
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.chip_sort -> FilterSheetFragment.show(supportFragmentManager)
		}
	}

	private fun initList(source: MangaSource, tags: Set<MangaTag>?) {
		val fm = supportFragmentManager
		val existingFragment = fm.findFragmentById(R.id.container)
		if (existingFragment is FilterOwner) {
			initFilter(existingFragment)
		} else {
			fm.commit {
				setReorderingAllowed(true)
				val fragment = if (source == MangaSource.LOCAL) {
					LocalListFragment.newInstance()
				} else {
					RemoteListFragment.newInstance(source)
				}
				replace(R.id.container, fragment)
				runOnCommit { initFilter(fragment) }
				if (!tags.isNullOrEmpty()) {
					runOnCommit(ApplyFilterRunnable(fragment, tags))
				}
			}
		}
	}

	private fun initFilter(filterOwner: FilterOwner) {
		if (viewBinding.containerFilter != null) {
			if (supportFragmentManager.findFragmentById(R.id.container_filter) == null) {
				supportFragmentManager.commit {
					setReorderingAllowed(true)
					replace(R.id.container_filter, FilterSheetFragment::class.java, null)
				}
			}
		} else if (viewBinding.containerFilterHeader != null) {
			if (supportFragmentManager.findFragmentById(R.id.container_filter_header) == null) {
				supportFragmentManager.commit {
					setReorderingAllowed(true)
					replace(R.id.container_filter_header, FilterHeaderFragment::class.java, null)
				}
			}
		}
		val chipSort = viewBinding.chipSort
		if (chipSort != null) {
			filterOwner.header.observe(this) {
				chipSort.setTextAndVisible(it.sortOrder?.titleRes ?: 0)
			}
		} else {
			filterOwner.header.map {
				it.textSummary
			}.flowOn(Dispatchers.Default)
				.observe(this) {
					supportActionBar?.subtitle = it
				}
		}
	}

	private class ApplyFilterRunnable(
		private val filterOwner: FilterOwner,
		private val tags: Set<MangaTag>,
	) : Runnable {

		override fun run() {
			filterOwner.applyFilter(tags)
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
