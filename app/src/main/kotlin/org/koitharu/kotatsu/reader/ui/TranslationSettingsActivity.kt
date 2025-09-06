package org.koitharu.kotatsu.reader.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.databinding.ActivityTranslationSettingsBinding
import org.koitharu.kotatsu.reader.domain.MangaTranslationPreference

@AndroidEntryPoint
class TranslationSettingsActivity : BaseActivity<ActivityTranslationSettingsBinding>(),
	OnListItemClickListener<MangaTranslationPreference> {

	private val viewModel by viewModels<TranslationSettingsViewModel>()
	private lateinit var adapter: TranslationPreferencesAdapter
	private lateinit var itemTouchHelper: ItemTouchHelper

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityTranslationSettingsBinding.inflate(layoutInflater))
		title = getString(R.string.translation_settings)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		setupRecyclerView()
		setupObservers()
	}

	private fun setupRecyclerView() {
		adapter = TranslationPreferencesAdapter(this)
		viewBinding.recyclerView.layoutManager = LinearLayoutManager(this)
		viewBinding.recyclerView.adapter = adapter

		// Set up drag-to-reorder functionality
		val callback = TranslationPreferenceItemTouchCallback(adapter) { newOrder ->
			viewModel.updatePreferencesOrder(newOrder)
		}
		itemTouchHelper = ItemTouchHelper(callback)
		itemTouchHelper.attachToRecyclerView(viewBinding.recyclerView)
	}

	private fun setupObservers() {
		lifecycleScope.launch {
			viewModel.preferences.collect { preferences ->
				adapter.submitList(preferences)
				viewBinding.textEmpty.isVisible = preferences.isEmpty()
			}
		}

		lifecycleScope.launch {
			viewModel.isLoading.collect { isLoading ->
				viewBinding.progressBar.isVisible = isLoading
			}
		}

		lifecycleScope.launch {
			viewModel.skipDecimalChapters.collect { skipDecimals ->
				viewBinding.checkboxSkipDecimals.isChecked = skipDecimals
			}
		}

		viewBinding.checkboxSkipDecimals.setOnCheckedChangeListener { _, isChecked ->
			viewModel.setSkipDecimalChapters(isChecked)
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.opt_translation_settings, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			android.R.id.home -> {
				finish()
				true
			}
			R.id.action_global_settings -> {
				// Navigate to reader settings translation section
				router.openReaderSettings()
				true
			}
			R.id.action_apply_default_languages -> {
				viewModel.applyDefaultLanguages()
				true
			}
			R.id.action_reset_defaults -> {
				viewModel.resetToDefaults()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onItemClick(item: MangaTranslationPreference, view: android.view.View) {
		// Toggle enabled state when item is clicked
		viewModel.togglePreferenceEnabled(item, !item.isEnabled)
	}

	fun onDragHandleTouched(viewHolder: RecyclerView.ViewHolder) {
		itemTouchHelper.startDrag(viewHolder)
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
		// Apply top padding to AppBarLayout for status bar
		viewBinding.toolbar.parent?.let { appBar ->
			(appBar as? View)?.updatePadding(top = bars.top)
		}
		// Apply side and bottom padding to RecyclerView
		viewBinding.recyclerView.updatePadding(
			left = bars.left,
			right = bars.right,
			bottom = bars.bottom,
		)
		return insets
	}
}