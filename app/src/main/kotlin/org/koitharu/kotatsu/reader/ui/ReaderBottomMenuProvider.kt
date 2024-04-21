package org.koitharu.kotatsu.reader.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentActivity
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.core.util.ext.isRtl
import org.koitharu.kotatsu.core.util.ext.setValueRounded
import org.koitharu.kotatsu.details.ui.pager.ChaptersPagesSheet
import org.koitharu.kotatsu.reader.ui.config.ReaderConfigSheet
import org.koitharu.kotatsu.reader.ui.pager.ReaderUiState
import org.koitharu.kotatsu.settings.SettingsActivity
import java.lang.ref.WeakReference

class ReaderBottomMenuProvider(
	private val activity: FragmentActivity,
	private val readerManager: ReaderManager,
	private val viewModel: ReaderViewModel,
	private val callback: ReaderNavigationCallback,
) : OnBackPressedCallback(false), MenuProvider, MenuItem.OnActionExpandListener {

	private var expandedItemRef: WeakReference<MenuItem>? = null

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_reader_bottom, menu)
		onPrepareMenu(menu) // fix, not called in toolbar
	}

	override fun onPrepareMenu(menu: Menu) {
		val shouldExpandSlider = expandedItemRef != null

		val hasPages = viewModel.content.value.pages.isNotEmpty()
		menu.findItem(R.id.action_pages_thumbs).isVisible = hasPages

		menu.findItem(R.id.action_slider)?.run {
			val state = viewModel.uiState.value?.takeIf { it.isSliderAvailable() }
			isVisible = state != null
			setOnActionExpandListener(this@ReaderBottomMenuProvider)
			if (state != null) {
				(actionView as? Slider)?.setupPagesSlider(state)
			}
			if (shouldExpandSlider) {
				expandActionView()
			}
		}
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_settings -> {
				activity.startActivity(SettingsActivity.newReaderSettingsIntent(activity))
				true
			}

			R.id.action_pages_thumbs -> {
				ChaptersPagesSheet.show(activity.supportFragmentManager, ChaptersPagesSheet.TAB_PAGES)
				true
			}

			R.id.action_options -> {
				viewModel.saveCurrentState(readerManager.currentReader?.getCurrentState())
				val currentMode = readerManager.currentMode ?: return false
				ReaderConfigSheet.show(activity.supportFragmentManager, currentMode)
				true
			}

			else -> false
		}
	}

	override fun handleOnBackPressed() {
		expandedItemRef?.get()?.collapseActionView()
	}

	override fun onMenuItemActionExpand(item: MenuItem): Boolean {
		expandedItemRef = WeakReference(item)
		isEnabled = true
		return true
	}

	override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
		expandedItemRef = null
		isEnabled = false
		return true
	}

	fun collapseSlider() {
		expandedItemRef?.get()?.collapseActionView()
	}

	fun isSliderExpanded(): Boolean {
		return expandedItemRef?.get()?.isActionViewExpanded == true
	}

	fun updateState(state: ReaderUiState?): Boolean {
		if (state == null || !state.isSliderAvailable()) {
			return false
		}
		val slider = (expandedItemRef?.get()?.actionView as? Slider) ?: return false
		slider.valueTo = state.totalPages.toFloat() - 1
		slider.setValueRounded(state.currentPage.toFloat())
		return true
	}

	private fun Slider.setupPagesSlider(state: ReaderUiState) {
		isRtl = viewModel.readerMode.value == ReaderMode.REVERSED
		valueTo = state.totalPages.toFloat() - 1
		setValueRounded(state.currentPage.toFloat())
		labelBehavior = LabelFormatter.LABEL_FLOATING
		isTickVisible = true
		setLabelFormatter(PageLabelFormatter())
		ReaderSliderListener(viewModel, callback).attachToSlider(this)
	}
}
