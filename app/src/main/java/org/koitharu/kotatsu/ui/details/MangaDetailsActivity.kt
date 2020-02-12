package org.koitharu.kotatsu.ui.details

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.net.toFile
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_details.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.ui.common.BaseActivity
import org.koitharu.kotatsu.ui.download.DownloadService
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
		invalidateOptionsMenu()
	}

	override fun onHistoryChanged(history: MangaHistory?) = Unit

	override fun onFavouriteChanged(categories: List<FavouriteCategory>) = Unit

	override fun onLoadingStateChanged(isLoading: Boolean) = Unit

	override fun onError(e: Exception) {
		Snackbar.make(pager, e.getDisplayMessage(resources), Snackbar.LENGTH_LONG).show()
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.opt_details, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onPrepareOptionsMenu(menu: Menu): Boolean {
		menu.findItem(R.id.action_save).isEnabled =
			manga?.source != null && manga?.source != MangaSource.LOCAL
		return super.onPrepareOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.action_share -> {
			manga?.let {
				if (it.source == MangaSource.LOCAL) {
					ShareHelper.shareCbz(this, Uri.parse(it.url).toFile())
				} else {
					ShareHelper.shareMangaLink(this, it)
				}
			}
			true
		}
		R.id.action_save -> {
			manga?.let {
				DownloadService.start(this, it)
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