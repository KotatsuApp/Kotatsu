package org.koitharu.kotatsu.filter.ui.tags

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.sheet.AdaptiveSheetBehavior
import org.koitharu.kotatsu.core.ui.sheet.AdaptiveSheetCallback
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.showDistinct
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.SheetTagsBinding
import org.koitharu.kotatsu.filter.ui.FilterOwner
import org.koitharu.kotatsu.filter.ui.model.TagCatalogItem

@AndroidEntryPoint
class TagsCatalogSheet : BaseAdaptiveSheet<SheetTagsBinding>(), OnListItemClickListener<TagCatalogItem>, TextWatcher,
	AdaptiveSheetCallback, View.OnFocusChangeListener, TextView.OnEditorActionListener {

	private val viewModel by viewModels<TagsCatalogViewModel>(
		extrasProducer = {
			defaultViewModelCreationExtras.withCreationCallback<TagsCatalogViewModel.Factory> { factory ->
				factory.create(
					filter = (requireActivity() as FilterOwner).filter,
					isExcludeTag = requireArguments().getBoolean(ARG_EXCLUDE),
				)
			}
		},
	)

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): SheetTagsBinding {
		return SheetTagsBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: SheetTagsBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val adapter = TagsCatalogAdapter(this)
		binding.recyclerView.adapter = adapter
		binding.recyclerView.setHasFixedSize(true)
		binding.editSearch.setText(viewModel.searchQuery.value)
		binding.editSearch.addTextChangedListener(this)
		binding.editSearch.onFocusChangeListener = this
		binding.editSearch.setOnEditorActionListener(this)
		viewModel.content.observe(viewLifecycleOwner, adapter)
		addSheetCallback(this, viewLifecycleOwner)
		disableFitToContents()
	}

	override fun onItemClick(item: TagCatalogItem, view: View) {
		viewModel.handleTagClick(item.tag, item.isChecked)
	}

	override fun onFocusChange(v: View?, hasFocus: Boolean) {
		setExpanded(
			isExpanded = hasFocus || isExpanded,
			isLocked = hasFocus,
		)
	}

	override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
		return if (actionId == EditorInfo.IME_ACTION_SEARCH) {
			v.clearFocus()
			true
		} else {
			false
		}
	}

	override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

	override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

	override fun afterTextChanged(s: Editable?) {
		val q = s?.toString().orEmpty()
		viewModel.searchQuery.value = q
	}

	override fun onStateChanged(sheet: View, newState: Int) {
		viewBinding?.recyclerView?.isFastScrollerEnabled = newState == AdaptiveSheetBehavior.STATE_EXPANDED
	}

	companion object {

		private const val TAG = "TagsCatalogSheet"
		private const val ARG_EXCLUDE = "exclude"

		fun show(fm: FragmentManager, isExcludeTag: Boolean) = TagsCatalogSheet().withArgs(1) {
			putBoolean(ARG_EXCLUDE, isExcludeTag)
		}.showDistinct(fm, TAG)
	}
}
