package org.koitharu.kotatsu.picker.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.DialogErrorObserver
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.ext.consume
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.databinding.ActivityPickerBinding
import org.koitharu.kotatsu.main.ui.owners.AppBarOwner
import org.koitharu.kotatsu.main.ui.owners.SnackbarOwner
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.picker.ui.manga.MangaPickerFragment
import org.koitharu.kotatsu.picker.ui.page.PagePickerFragment
import org.koitharu.kotatsu.reader.ui.PageSaveHelper
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class PageImagePickActivity : BaseActivity<ActivityPickerBinding>(),
	AppBarOwner,
	SnackbarOwner {

	@Inject
	lateinit var pageSaveHelperFactory: PageSaveHelper.Factory

	override val appBar: AppBarLayout
		get() = viewBinding.appbar

	override val snackbarHost: CoordinatorLayout
		get() = viewBinding.root

	private lateinit var pageSaveHelper: PageSaveHelper
	private val viewModel by viewModels<PageImagePickViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityPickerBinding.inflate(layoutInflater))
		setDisplayHomeAsUp(isEnabled = true, showUpAsClose = false)
		pageSaveHelper = pageSaveHelperFactory.create(this)
		viewModel.onError.observeEvent(this, DialogErrorObserver(viewBinding.container, null))
		viewModel.onFileReady.observeEvent(this, ::finishWithResult)
		viewModel.isLoading.observe(this, ::onLoadingStateChanged)
		val fm = supportFragmentManager
		if (fm.findFragmentById(R.id.container) == null) {
			fm.commit {
				setReorderingAllowed(true)
				if (intent?.hasExtra(AppRouter.KEY_MANGA) == true) {
					replace(R.id.container, PagePickerFragment::class.java, intent.extras)
				} else {
					replace(R.id.container, MangaPickerFragment::class.java, null)
				}
			}
		}
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val typeMask = WindowInsetsCompat.Type.systemBars()
		val bars = insets.getInsets(typeMask)
		viewBinding.appbar.updatePadding(
			left = bars.left,
			right = bars.right,
			top = bars.top,
		)
		return insets.consume(v, typeMask, top = true)
	}

	fun onMangaPicked(manga: Manga) {
		val args = Bundle(1)
		args.putLong(AppRouter.KEY_ID, manga.id)
		supportFragmentManager.commit {
			setReorderingAllowed(true)
			replace(R.id.container, PagePickerFragment::class.java, args)
			addToBackStack(null)
		}
	}

	fun onPagePicked(manga: Manga, page: ReaderPage) {
		val task = PageSaveHelper.Task(
			manga = manga,
			chapterId = page.chapterId,
			pageNumber = page.index + 1,
			page = page.toMangaPage(),
		)
		viewModel.savePageToTempFile(pageSaveHelper, task)
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		viewBinding.container.isGone = isLoading
		viewBinding.progressBar.isVisible = isLoading
	}

	private fun finishWithResult(file: File) {
		val uri = FileProvider.getUriForFile(applicationContext, "${BuildConfig.APPLICATION_ID}.files", file)
		val result = Intent()
		result.setData(uri)
		result.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		setResult(RESULT_OK, result)
		finish()
	}
}
