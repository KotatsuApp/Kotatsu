package org.koitharu.kotatsu.scrobbling.common.ui.config

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import coil3.ImageLoader
import coil3.request.error
import coil3.request.fallback
import coil3.request.placeholder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.disposeImageRequest
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.showOrHide
import org.koitharu.kotatsu.databinding.ActivityScrobblerConfigBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.list.ui.adapter.TypedListSpacingDecoration
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerService
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerUser
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo
import org.koitharu.kotatsu.scrobbling.common.ui.config.adapter.ScrobblingMangaAdapter
import javax.inject.Inject
import com.google.android.material.R as materialR

@AndroidEntryPoint
class ScrobblerConfigActivity : BaseActivity<ActivityScrobblerConfigBinding>(),
	OnListItemClickListener<ScrobblingInfo>, View.OnClickListener {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel: ScrobblerConfigViewModel by viewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityScrobblerConfigBinding.inflate(layoutInflater))
		setTitle(viewModel.titleResId)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		val listAdapter = ScrobblingMangaAdapter(this, coil, this)
		with(viewBinding.recyclerView) {
			adapter = listAdapter
			setHasFixedSize(true)
			val decoration = TypedListSpacingDecoration(context, false)
			addItemDecoration(decoration)
		}
		viewBinding.imageViewAvatar.setOnClickListener(this)

		viewModel.content.observe(this, listAdapter)
		viewModel.user.observe(this, this::onUserChanged)
		viewModel.isLoading.observe(this, this::onLoadingStateChanged)
		viewModel.onError.observeEvent(this, SnackbarErrorObserver(viewBinding.recyclerView, null))
		viewModel.onLoggedOut.observeEvent(this) {
			finishAfterTransition()
		}

		processIntent(intent)
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		setIntent(intent)
		processIntent(intent)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		val rv = viewBinding.recyclerView
		rv.updatePadding(
			left = insets.left + rv.paddingTop,
			right = insets.right + rv.paddingTop,
			bottom = insets.bottom + rv.paddingTop,
		)
	}

	override fun onItemClick(item: ScrobblingInfo, view: View) {
		startActivity(
			DetailsActivity.newIntent(this, item.mangaId),
		)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.imageView_avatar -> showUserDialog()
		}
	}

	private fun processIntent(intent: Intent) {
		if (intent.action == Intent.ACTION_VIEW) {
			val uri = intent.data ?: return
			val code = uri.getQueryParameter("code")
			if (!code.isNullOrEmpty()) {
				viewModel.onAuthCodeReceived(code)
			}
		}
	}

	private fun onUserChanged(user: ScrobblerUser?) {
		if (user == null) {
			viewBinding.imageViewAvatar.disposeImageRequest()
			viewBinding.imageViewAvatar.setImageResource(materialR.drawable.abc_ic_menu_overflow_material)
			return
		}
		viewBinding.imageViewAvatar.newImageRequest(this, user.avatar)
			?.placeholder(R.drawable.bg_badge_empty)
			?.fallback(R.drawable.ic_shortcut_default)
			?.error(R.drawable.ic_shortcut_default)
			?.enqueueWith(coil)
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		viewBinding.progressBar.showOrHide(isLoading)
	}

	private fun showUserDialog() {
		MaterialAlertDialogBuilder(this)
			.setTitle(title)
			.setMessage(getString(R.string.logged_in_as, viewModel.user.value?.nickname))
			.setNegativeButton(R.string.close, null)
			.setPositiveButton(R.string.logout) { _, _ ->
				viewModel.logout()
			}.show()
	}

	companion object {

		const val EXTRA_SERVICE_ID = "service"

		const val HOST_SHIKIMORI_AUTH = "shikimori-auth"
		const val HOST_ANILIST_AUTH = "anilist-auth"
		const val HOST_MAL_AUTH = "mal-auth"
		const val HOST_KITSU_AUTH = "kitsu-auth"

		fun newIntent(context: Context, service: ScrobblerService) =
			Intent(context, ScrobblerConfigActivity::class.java)
				.putExtra(EXTRA_SERVICE_ID, service.id)
	}
}
