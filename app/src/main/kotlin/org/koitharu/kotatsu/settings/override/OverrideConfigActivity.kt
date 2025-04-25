package org.koitharu.kotatsu.settings.override

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import coil3.ImageLoader
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.model.MangaOverride
import org.koitharu.kotatsu.core.util.ext.consumeAll
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.tryLaunch
import org.koitharu.kotatsu.databinding.ActivityOverrideEditBinding
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.ifNullOrEmpty
import javax.inject.Inject
import androidx.appcompat.R as appcompatR
import com.google.android.material.R as materialR

@AndroidEntryPoint
class OverrideConfigActivity : BaseActivity<ActivityOverrideEditBinding>(), View.OnClickListener {

	private val viewModel: OverrideConfigViewModel by viewModels()

	private val pickCoverFileLauncher = registerForActivityResult(
		PickVisualMedia(),
	) { uri ->
		if (uri != null) {
			contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
			viewModel.updateCover(uri.toString())
		}
	}

	@Inject
	lateinit var coil: ImageLoader

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityOverrideEditBinding.inflate(layoutInflater))
		setDisplayHomeAsUp(isEnabled = true, showUpAsClose = true)
		viewBinding.buttonDone.setOnClickListener(this)
		viewBinding.buttonPickFile.setOnClickListener(this)
		viewBinding.buttonPickPage.setOnClickListener(this)
		viewBinding.buttonResetCover.setOnClickListener(this)
		viewBinding.layoutName.setEndIconOnClickListener(this)
		viewModel.data.filterNotNull().observe(this, ::onDataChanged)
		viewModel.onSaved.observeEvent(this) { finishAfterTransition() }
		viewModel.isLoading.observe(this, ::onLoadingStateChanged)
		viewModel.onError.observeEvent(this, ::onError)
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val typeMask = WindowInsetsCompat.Type.systemBars()
		val barsInsets = insets.getInsets(typeMask)
		viewBinding.root.setPadding(
			barsInsets.left,
			barsInsets.top,
			barsInsets.right,
			barsInsets.bottom,
		)
		return insets.consumeAll(typeMask)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_done -> viewModel.save(
				title = viewBinding.editName.text?.toString()?.trim(),
			)

			materialR.id.text_input_end_icon -> viewBinding.editName.text?.clear()

			R.id.button_reset_cover -> viewModel.updateCover(null)
			R.id.button_pick_file -> {
				val request = PickVisualMediaRequest.Builder()
					.setMediaType(PickVisualMedia.ImageOnly)
					.setAccentColor(getThemeColor(appcompatR.attr.colorAccent).toLong())
					.build()
				if (!pickCoverFileLauncher.tryLaunch(request)) {
					Snackbar.make(
						viewBinding.imageViewCover,
						R.string.operation_not_supported,
						Snackbar.LENGTH_SHORT,
					).show()
				}
			}
		}
	}

	private fun onDataChanged(data: Pair<Manga, MangaOverride>) {
		val (manga, override) = data
		viewBinding.imageViewCover.setImageAsync(override.coverUrl.ifNullOrEmpty { manga.coverUrl }, manga)
		viewBinding.layoutName.placeholderText = manga.title
		if (viewBinding.editName.tag == null) {
			viewBinding.editName.setText(override.title)
			viewBinding.editName.tag = override.title
		}
		viewBinding.buttonResetCover.isEnabled = !override.coverUrl.isNullOrEmpty()
	}

	private fun onError(e: Throwable) {
		viewBinding.textViewError.text = e.getDisplayMessage(resources)
		viewBinding.textViewError.isVisible = true
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		viewBinding.buttonDone.isEnabled = !isLoading
		viewBinding.editName.isEnabled = !isLoading
		if (isLoading) {
			viewBinding.textViewError.isVisible = false
		}
	}
}
