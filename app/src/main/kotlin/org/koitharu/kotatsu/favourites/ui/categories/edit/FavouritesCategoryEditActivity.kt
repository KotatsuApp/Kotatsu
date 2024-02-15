package org.koitharu.kotatsu.favourites.ui.categories.edit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.util.DefaultTextWatcher
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.getSerializableCompat
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.setChecked
import org.koitharu.kotatsu.core.util.ext.sortedByOrdinal
import org.koitharu.kotatsu.databinding.ActivityCategoryEditBinding
import org.koitharu.kotatsu.list.domain.ListSortOrder
import com.google.android.material.R as materialR

@AndroidEntryPoint
class FavouritesCategoryEditActivity :
	BaseActivity<ActivityCategoryEditBinding>(),
	AdapterView.OnItemClickListener,
	View.OnClickListener,
	DefaultTextWatcher {

	private val viewModel by viewModels<FavouritesCategoryEditViewModel>()
	private var selectedSortOrder: ListSortOrder? = null
	private val sortOrders = ListSortOrder.FAVORITES.sortedByOrdinal()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityCategoryEditBinding.inflate(layoutInflater))
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setHomeAsUpIndicator(materialR.drawable.abc_ic_clear_material)
		}
		initSortSpinner()
		viewBinding.buttonDone.setOnClickListener(this)
		viewBinding.editName.addTextChangedListener(this)
		afterTextChanged(viewBinding.editName.text)

		viewModel.onSaved.observeEvent(this) { finishAfterTransition() }
		viewModel.category.observe(this, ::onCategoryChanged)
		viewModel.isLoading.observe(this, ::onLoadingStateChanged)
		viewModel.onError.observeEvent(this, ::onError)
		viewModel.isTrackerEnabled.observe(this) {
			viewBinding.switchTracker.isVisible = it
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putSerializable(KEY_SORT_ORDER, selectedSortOrder)
	}

	override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)
		savedInstanceState.getSerializableCompat<ListSortOrder>(KEY_SORT_ORDER)?.let { 
		    selectedSortOrder = it
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_done -> viewModel.save(
				title = viewBinding.editName.text?.toString()?.trim().orEmpty(),
				sortOrder = getSelectedSortOrder(),
				isTrackerEnabled = viewBinding.switchTracker.isChecked,
				isVisibleOnShelf = viewBinding.switchShelf.isChecked,
			)
		}
	}

	override fun afterTextChanged(s: Editable?) {
		viewBinding.buttonDone.isEnabled = !s.isNullOrBlank()
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		viewBinding.scrollView.updatePadding(
			bottom = insets.bottom,
		)
		viewBinding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			topMargin = insets.top
		}
	}

	override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
		selectedSortOrder = sortOrders.getOrNull(position)
	}

	private fun onCategoryChanged(category: FavouriteCategory?) {
		setTitle(if (category == null) R.string.create_category else R.string.edit_category)
		if (selectedSortOrder != null) {
			return
		}
		viewBinding.editName.setText(category?.title)
		selectedSortOrder = category?.order
		val sortText = getString((category?.order ?: ListSortOrder.NEWEST).titleResId)
		viewBinding.editSort.setText(sortText, false)
		viewBinding.switchTracker.setChecked(category?.isTrackingEnabled ?: true, false)
		viewBinding.switchShelf.setChecked(category?.isVisibleInLibrary ?: true, false)
	}

	private fun onError(e: Throwable) {
		viewBinding.textViewError.text = e.getDisplayMessage(resources)
		viewBinding.textViewError.isVisible = true
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		viewBinding.editSort.isEnabled = !isLoading
		viewBinding.editName.isEnabled = !isLoading
		viewBinding.switchTracker.isEnabled = !isLoading
		viewBinding.switchShelf.isEnabled = !isLoading
		if (isLoading) {
			viewBinding.textViewError.isVisible = false
		}
	}

	private fun initSortSpinner() {
		val entries = sortOrders.map { getString(it.titleResId) }
		val adapter = SortAdapter(this, entries)
		viewBinding.editSort.setAdapter(adapter)
		viewBinding.editSort.onItemClickListener = this
	}

	private fun getSelectedSortOrder(): ListSortOrder {
		selectedSortOrder?.let { return it }
		val entries = sortOrders.map { getString(it.titleResId) }
		val index = entries.indexOf(viewBinding.editSort.text.toString())
		return sortOrders.getOrNull(index) ?: ListSortOrder.NEWEST
	}

	private class SortAdapter(
		context: Context,
		entries: List<String>,
	) : ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, entries) {

		override fun getFilter(): Filter = EmptyFilter

		private object EmptyFilter : Filter() {
			override fun performFiltering(constraint: CharSequence?) = FilterResults()
			override fun publishResults(constraint: CharSequence?, results: FilterResults?) = Unit
		}
	}

	companion object {

		const val EXTRA_ID = "id"
		const val NO_ID = -1L
		private const val KEY_SORT_ORDER = "sort"

		fun newIntent(context: Context, id: Long = NO_ID): Intent {
			return Intent(context, FavouritesCategoryEditActivity::class.java)
				.putExtra(EXTRA_ID, id)
		}
	}
}
