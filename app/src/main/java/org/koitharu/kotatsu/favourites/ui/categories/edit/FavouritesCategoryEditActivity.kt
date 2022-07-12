package org.koitharu.kotatsu.favourites.ui.categories.edit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.ui.titleRes
import org.koitharu.kotatsu.databinding.ActivityCategoryEditBinding
import org.koitharu.kotatsu.favourites.ui.categories.CategoriesActivity
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

class FavouritesCategoryEditActivity : BaseActivity<ActivityCategoryEditBinding>(), AdapterView.OnItemClickListener,
	View.OnClickListener {

	private val viewModel by viewModel<FavouritesCategoryEditViewModel> {
		parametersOf(intent.getLongExtra(EXTRA_ID, NO_ID))
	}
	private var selectedSortOrder: SortOrder? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityCategoryEditBinding.inflate(layoutInflater))
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setHomeAsUpIndicator(com.google.android.material.R.drawable.abc_ic_clear_material)
		}
		initSortSpinner()
		binding.buttonDone.setOnClickListener(this)

		viewModel.onSaved.observe(this) { finishAfterTransition() }
		viewModel.category.observe(this, ::onCategoryChanged)
		viewModel.isLoading.observe(this, ::onLoadingStateChanged)
		viewModel.onError.observe(this, ::onError)
		viewModel.isTrackerEnabled.observe(this) {
			binding.switchTracker.isVisible = it
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putSerializable(KEY_SORT_ORDER, selectedSortOrder)
	}

	override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)
		val order = savedInstanceState.getSerializable(KEY_SORT_ORDER)
		if (order != null && order is SortOrder) {
			selectedSortOrder = order
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_done -> viewModel.save(
				title = binding.editName.text?.toString().orEmpty(),
				sortOrder = getSelectedSortOrder(),
				isTrackerEnabled = binding.switchTracker.isChecked,
			)
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.scrollView.updatePadding(
			left = insets.left,
			right = insets.right,
			bottom = insets.bottom,
		)
		binding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			topMargin = insets.top
		}
	}

	override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
		selectedSortOrder = CategoriesActivity.SORT_ORDERS.getOrNull(position)
	}

	private fun onCategoryChanged(category: FavouriteCategory?) {
		setTitle(if (category == null) R.string.create_category else R.string.edit_category)
		if (selectedSortOrder != null) {
			return
		}
		binding.editName.setText(category?.title)
		selectedSortOrder = category?.order
		val sortText = getString((category?.order ?: SortOrder.NEWEST).titleRes)
		binding.editSort.setText(sortText, false)
		binding.switchTracker.isChecked = category?.isTrackingEnabled ?: true
	}

	private fun onError(e: Throwable) {
		binding.textViewError.text = e.getDisplayMessage(resources)
		binding.textViewError.isVisible = true
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		binding.editSort.isEnabled = !isLoading
		binding.editName.isEnabled = !isLoading
		binding.switchTracker.isEnabled = !isLoading
		if (isLoading) {
			binding.textViewError.isVisible = false
		}
	}

	private fun initSortSpinner() {
		val entries = CategoriesActivity.SORT_ORDERS.map { getString(it.titleRes) }
		val adapter = SortAdapter(this, entries)
		binding.editSort.setAdapter(adapter)
		binding.editSort.onItemClickListener = this
	}

	private fun getSelectedSortOrder(): SortOrder {
		selectedSortOrder?.let { return it }
		val entries = CategoriesActivity.SORT_ORDERS.map { getString(it.titleRes) }
		val index = entries.indexOf(binding.editSort.text.toString())
		return CategoriesActivity.SORT_ORDERS.getOrNull(index) ?: SortOrder.NEWEST
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

		private const val EXTRA_ID = "id"
		private const val KEY_SORT_ORDER = "sort"
		private const val NO_ID = -1L

		fun newIntent(context: Context, id: Long = NO_ID): Intent {
			return Intent(context, FavouritesCategoryEditActivity::class.java)
				.putExtra(EXTRA_ID, id)
		}
	}
}