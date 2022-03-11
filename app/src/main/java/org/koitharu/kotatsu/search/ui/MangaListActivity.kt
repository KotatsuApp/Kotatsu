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
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.databinding.ActivitySearchGlobalBinding
import org.koitharu.kotatsu.remotelist.ui.RemoteListFragment
import org.koitharu.kotatsu.remotelist.ui.RemoteListViewModel

class MangaListActivity : BaseActivity<ActivitySearchGlobalBinding>() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySearchGlobalBinding.inflate(layoutInflater))
		val tag = intent.getParcelableExtra<MangaTag>(EXTRA_TAG) ?: run {
			finishAfterTransition()
			return
		}
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val fm = supportFragmentManager
		if (fm.findFragmentById(R.id.container) == null) {
			fm.commit {
				val fragment = RemoteListFragment.newInstance(tag.source)
				replace(R.id.container, fragment)
				runOnCommit(ApplyFilterRunnable(fragment, tag))
			}
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
		binding.container.updatePadding(
			bottom = insets.bottom
		)
	}

	private class ApplyFilterRunnable(
		private val fragment: Fragment,
		private val tag: MangaTag,
	) : Runnable {

		override fun run() {
			val viewModel = fragment.getViewModel<RemoteListViewModel> {
				parametersOf(tag.source)
			}
			viewModel.applyFilter(setOf(tag))
		}
	}

	companion object {

		private const val EXTRA_TAG = "tag"

		fun newIntent(context: Context, tag: MangaTag) =
			Intent(context, MangaListActivity::class.java)
				.putExtra(EXTRA_TAG, tag)
	}
}