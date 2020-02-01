package org.koitharu.kotatsu.ui.main.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_details.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.ui.common.BaseActivity
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

class MangaDetailsActivity : BaseActivity(), MangaDetailsView {

	val presenter by moxyPresenter(factory = ::MangaDetailsPresenter)

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
		title = manga.title
	}

	override fun onLoadingStateChanged(isLoading: Boolean) = Unit

	override fun onError(e: Exception) {
		Snackbar.make(pager, e.getDisplayMessage(resources), Snackbar.LENGTH_LONG).show()
	}

	companion object {

		private const val EXTRA_MANGA = "manga"

		fun newIntent(context: Context, manga: Manga) = Intent(context, MangaDetailsActivity::class.java)
			.putExtra(EXTRA_MANGA, manga)
	}
}