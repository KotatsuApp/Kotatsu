package org.koitharu.kotatsu.scrobbling.common.ui.config

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import coil.ImageLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.decor.TypedSpacingItemDecoration
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.databinding.ActivityScrobblerConfigBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerService
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerUser
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo
import org.koitharu.kotatsu.scrobbling.common.ui.config.adapter.ScrobblingMangaAdapter
import org.koitharu.kotatsu.tracker.ui.feed.adapter.FeedAdapter
import org.koitharu.kotatsu.utils.ext.disposeImageRequest
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.hideCompat
import org.koitharu.kotatsu.utils.ext.newImageRequest
import org.koitharu.kotatsu.utils.ext.showCompat
import javax.inject.Inject

@AndroidEntryPoint
class ScrobblerConfigActivity : BaseActivity<ActivityScrobblerConfigBinding>(),
	OnListItemClickListener<ScrobblingInfo>, View.OnClickListener {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel: ScrobblerConfigViewModel by viewModels()

	private var paddingVertical = 0
	private var paddingHorizontal = 0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityScrobblerConfigBinding.inflate(layoutInflater))
		setTitle(viewModel.titleResId)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		val listAdapter = ScrobblingMangaAdapter(this, coil, this)
		with(binding.recyclerView) {
			adapter = listAdapter
			setHasFixedSize(true)
			val spacing = resources.getDimensionPixelOffset(R.dimen.list_spacing)
			paddingHorizontal = spacing
			paddingVertical = resources.getDimensionPixelOffset(R.dimen.grid_spacing_outer)
			val decoration = TypedSpacingItemDecoration(
				FeedAdapter.ITEM_TYPE_FEED to 0,
				fallbackSpacing = spacing,
			)
			addItemDecoration(decoration)
		}
		binding.imageViewAvatar.setOnClickListener(this)

		viewModel.content.observe(this, listAdapter::setItems)
		viewModel.user.observe(this, this::onUserChanged)
		viewModel.isLoading.observe(this, this::onLoadingStateChanged)
		viewModel.onError.observe(this, SnackbarErrorObserver(binding.recyclerView, null))
		viewModel.onLoggedOut.observe(this) {
			finishAfterTransition()
		}

		processIntent(intent)
	}

	override fun onNewIntent(intent: Intent?) {
		super.onNewIntent(intent)
		if (intent != null) {
			setIntent(intent)
			processIntent(intent)
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.recyclerView.updatePadding(
			left = insets.left + paddingHorizontal,
			right = insets.right + paddingHorizontal,
			bottom = insets.bottom + paddingVertical,
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
			binding.imageViewAvatar.disposeImageRequest()
			binding.imageViewAvatar.isVisible = false
			return
		}
		binding.imageViewAvatar.isVisible = true
		binding.imageViewAvatar.newImageRequest(this, user.avatar)
			?.enqueueWith(coil)
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		binding.progressBar.run {
			if (isLoading) {
				showCompat()
			} else {
				hideCompat()
			}
		}
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

		fun newIntent(context: Context, service: ScrobblerService) =
			Intent(context, ScrobblerConfigActivity::class.java)
				.putExtra(EXTRA_SERVICE_ID, service.id)
	}
}
