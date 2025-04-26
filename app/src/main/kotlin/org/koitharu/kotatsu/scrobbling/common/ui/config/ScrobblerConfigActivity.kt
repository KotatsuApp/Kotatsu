package org.koitharu.kotatsu.scrobbling.common.ui.config

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.consumeAllSystemBarsInsets
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.showOrHide
import org.koitharu.kotatsu.core.util.ext.systemBarsInsets
import org.koitharu.kotatsu.databinding.ActivityScrobblerConfigBinding
import org.koitharu.kotatsu.list.ui.adapter.TypedListSpacingDecoration
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerUser
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo
import org.koitharu.kotatsu.scrobbling.common.ui.config.adapter.ScrobblingMangaAdapter
import androidx.appcompat.R as appcompatR

@AndroidEntryPoint
class ScrobblerConfigActivity : BaseActivity<ActivityScrobblerConfigBinding>(),
	OnListItemClickListener<ScrobblingInfo>, View.OnClickListener {

	private val viewModel: ScrobblerConfigViewModel by viewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityScrobblerConfigBinding.inflate(layoutInflater))
		setTitle(viewModel.titleResId)
		setDisplayHomeAsUp(isEnabled = true, showUpAsClose = false)

		val listAdapter = ScrobblingMangaAdapter(this)
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

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val barsInsets = insets.systemBarsInsets
		val basePadding = v.resources.getDimensionPixelOffset(R.dimen.list_spacing_normal)
		viewBinding.appbar.updatePadding(
			top = barsInsets.top,
			left = barsInsets.left,
			right = barsInsets.right,
		)
		viewBinding.recyclerView.setPadding(
			barsInsets.left + basePadding,
			barsInsets.top + basePadding,
			barsInsets.right + basePadding,
			barsInsets.bottom + basePadding,
		)
		return insets.consumeAllSystemBarsInsets()
	}

	override fun onItemClick(item: ScrobblingInfo, view: View) {
		router.openDetails(item.mangaId)
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
			viewBinding.imageViewAvatar.disposeImage()
			viewBinding.imageViewAvatar.setImageResource(appcompatR.drawable.abc_ic_menu_overflow_material)
			return
		}
		viewBinding.imageViewAvatar.setImageAsync(user.avatar)
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
		const val HOST_SHIKIMORI_AUTH = "shikimori-auth"
		const val HOST_ANILIST_AUTH = "anilist-auth"
		const val HOST_MAL_AUTH = "mal-auth"
		const val HOST_KITSU_AUTH = "kitsu-auth"
	}
}
