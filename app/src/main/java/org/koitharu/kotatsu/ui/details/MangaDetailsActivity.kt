package org.koitharu.kotatsu.ui.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_details.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.ui.common.BaseActivity
import org.koitharu.kotatsu.utils.ShareHelper
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

class MangaDetailsActivity : BaseActivity(), MangaDetailsView {

	val presenter by moxyPresenter(factory = ::MangaDetailsPresenter)

	private var manga: Manga? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_details)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		pager.adapter = MangaDetailsAdapter(resources, supportFragmentManager)
		tabs.setupWithViewPager(pager)
		intent?.getParcelableExtra<Manga>(EXTRA_MANGA)?.let {
			presenter.loadDetails(it)
		} ?: finish()
	}

	override fun onMangaUpdated(manga: Manga) {
		this.manga = manga
		title = manga.title
	}

	override fun onHistoryChanged(history: MangaHistory?) = Unit

	override fun onLoadingStateChanged(isLoading: Boolean) = Unit

	override fun onError(e: Exception) {
		Snackbar.make(pager, e.getDisplayMessage(resources), Snackbar.LENGTH_LONG).show()
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.opt_details, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.action_share -> {
			manga?.let {
				ShareHelper.shareMangaLink(this, it)
			}
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	companion object {

		private const val EXTRA_MANGA = "manga"

		fun newIntent(context: Context, manga: Manga) =
			Intent(context, MangaDetailsActivity::class.java)
				.putExtra(EXTRA_MANGA, manga)
	}
}