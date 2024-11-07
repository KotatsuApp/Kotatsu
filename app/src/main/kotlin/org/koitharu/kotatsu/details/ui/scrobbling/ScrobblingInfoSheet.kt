package org.koitharu.kotatsu.details.ui.scrobbling

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.net.toUri
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import coil3.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.util.ext.defaultPlaceholders
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.sanitize
import org.koitharu.kotatsu.core.util.ext.scaleUpActivityOptionsOf
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.SheetScrobblingBinding
import org.koitharu.kotatsu.details.ui.DetailsViewModel
import org.koitharu.kotatsu.image.ui.ImageActivity
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingStatus
import org.koitharu.kotatsu.scrobbling.common.ui.selector.ScrobblingSelectorSheet
import javax.inject.Inject

@AndroidEntryPoint
class ScrobblingInfoSheet :
	BaseAdaptiveSheet<SheetScrobblingBinding>(),
	AdapterView.OnItemSelectedListener,
	RatingBar.OnRatingBarChangeListener,
	View.OnClickListener,
	PopupMenu.OnMenuItemClickListener {

	private val viewModel by activityViewModels<DetailsViewModel>()
	private var scrobblerIndex: Int = -1

	@Inject
	lateinit var coil: ImageLoader

	private var menu: PopupMenu? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		scrobblerIndex = requireArguments().getInt(ARG_INDEX, scrobblerIndex)
	}

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): SheetScrobblingBinding {
		return SheetScrobblingBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: SheetScrobblingBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		viewModel.scrobblingInfo.observe(viewLifecycleOwner, ::onScrobblingInfoChanged)
		viewModel.onError.observeEvent(viewLifecycleOwner) {
			Toast.makeText(binding.root.context, it.getDisplayMessage(binding.root.resources), Toast.LENGTH_SHORT)
				.show()
		}

		binding.spinnerStatus.onItemSelectedListener = this
		binding.ratingBar.onRatingBarChangeListener = this
		binding.buttonMenu.setOnClickListener(this)
		binding.imageViewCover.setOnClickListener(this)
		binding.textViewDescription.movementMethod = LinkMovementMethodCompat.getInstance()

		menu = PopupMenu(binding.root.context, binding.buttonMenu).apply {
			inflate(R.menu.opt_scrobbling)
			setOnMenuItemClickListener(this@ScrobblingInfoSheet)
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		menu = null
	}

	override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
		viewModel.updateScrobbling(
			index = scrobblerIndex,
			rating = requireViewBinding().ratingBar.rating / requireViewBinding().ratingBar.numStars,
			status = ScrobblingStatus.entries.getOrNull(position),
		)
	}

	override fun onNothingSelected(parent: AdapterView<*>?) = Unit

	override fun onRatingChanged(ratingBar: RatingBar, rating: Float, fromUser: Boolean) {
		if (fromUser) {
			viewModel.updateScrobbling(
				index = scrobblerIndex,
				rating = rating / ratingBar.numStars,
				status = ScrobblingStatus.entries.getOrNull(requireViewBinding().spinnerStatus.selectedItemPosition),
			)
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_menu -> menu?.show()
			R.id.imageView_cover -> {
				val coverUrl = viewModel.scrobblingInfo.value.getOrNull(scrobblerIndex)?.coverUrl ?: return
				val options = scaleUpActivityOptionsOf(v)
				startActivity(ImageActivity.newIntent(v.context, coverUrl, null), options)
			}
		}
	}

	private fun onScrobblingInfoChanged(scrobblings: List<ScrobblingInfo>) {
		val scrobbling = scrobblings.getOrNull(scrobblerIndex)
		if (scrobbling == null) {
			dismissAllowingStateLoss()
			return
		}
		val binding = viewBinding ?: return
		binding.textViewTitle.text = scrobbling.title
		binding.ratingBar.rating = scrobbling.rating * binding.ratingBar.numStars
		binding.textViewDescription.text = scrobbling.description?.sanitize()
		binding.spinnerStatus.setSelection(scrobbling.status?.ordinal ?: -1)
		binding.imageViewLogo.contentDescription = getString(scrobbling.scrobbler.titleResId)
		binding.imageViewLogo.setImageResource(scrobbling.scrobbler.iconResId)
		binding.imageViewCover.newImageRequest(viewLifecycleOwner, scrobbling.coverUrl)?.apply {
			defaultPlaceholders(binding.imageViewCover.context)
			enqueueWith(coil)
		}
	}

	override fun onMenuItemClick(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.action_browser -> {
				val url = viewModel.scrobblingInfo.value.getOrNull(scrobblerIndex)?.externalUrl ?: return false
				val intent = Intent(Intent.ACTION_VIEW, url.toUri())
				startActivity(
					Intent.createChooser(intent, getString(R.string.open_in_browser)),
				)
			}

			R.id.action_unregister -> {
				viewModel.unregisterScrobbling(scrobblerIndex)
				dismiss()
			}

			R.id.action_edit -> {
				val manga = viewModel.manga.value ?: return false
				val scrobblerService = viewModel.scrobblingInfo.value.getOrNull(scrobblerIndex)?.scrobbler
				ScrobblingSelectorSheet.show(parentFragmentManager, manga, scrobblerService)
				dismiss()
			}
		}
		return true
	}

	companion object {

		private const val TAG = "ScrobblingInfoBottomSheet"
		private const val ARG_INDEX = "index"

		fun show(fm: FragmentManager, index: Int) = ScrobblingInfoSheet().withArgs(1) {
			putInt(ARG_INDEX, index)
		}.show(fm, TAG)
	}
}
