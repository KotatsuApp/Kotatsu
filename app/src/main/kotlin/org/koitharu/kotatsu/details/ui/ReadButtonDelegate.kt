package org.koitharu.kotatsu.details.ui

import android.content.Context
import android.graphics.Color
import android.text.style.DynamicDrawableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.RelativeSizeSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.view.MenuCompat
import androidx.core.view.get
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialSplitButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.nav.ReaderIntent
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.details.ui.model.HistoryInfo

class ReadButtonDelegate(
	private val splitButton: MaterialSplitButton,
	private val viewModel: DetailsViewModel,
	private val router: AppRouter,
) : View.OnClickListener, PopupMenu.OnMenuItemClickListener, PopupMenu.OnDismissListener {

	private val buttonRead = splitButton[0] as MaterialButton
	private val buttonMenu = splitButton[1] as MaterialButton

	private val context: Context
		get() = buttonRead.context

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_read -> openReader(isIncognitoMode = false)
			R.id.button_read_menu -> showMenu()
		}
	}

	override fun onMenuItemClick(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.action_incognito -> openReader(isIncognitoMode = true)
			R.id.action_forget -> viewModel.removeFromHistory()
			R.id.action_download -> {
				router.showDownloadDialog(
					manga = setOf(viewModel.getMangaOrNull() ?: return false),
					snackbarHost = splitButton,
				)
			}

			Menu.NONE -> {
				val branch = viewModel.branches.value.getOrNull(item.order) ?: return false
				viewModel.setSelectedBranch(branch.name)
			}

			else -> return false
		}
		return true
	}

	override fun onDismiss(menu: PopupMenu?) {
		buttonMenu.isChecked = false
	}

	fun attach(lifecycleOwner: LifecycleOwner) {
		buttonRead.setOnClickListener(this)
		buttonMenu.setOnClickListener(this)
		combine(viewModel.isLoading, viewModel.historyInfo, ::Pair)
			.observe(lifecycleOwner) { (isLoading, historyInfo) ->
				onHistoryChanged(isLoading, historyInfo)
			}
	}

	private fun showMenu() {
		val menu = PopupMenu(context, buttonMenu)
		menu.inflate(R.menu.popup_read)
		prepareMenu(menu.menu)
		menu.setOnMenuItemClickListener(this)
		menu.setForceShowIcon(true)
		menu.setOnDismissListener(this)
		if (menu.menu.hasVisibleItems()) {
			buttonMenu.isChecked = true
			menu.show()
		} else {
			buttonMenu.isChecked = false
		}
	}

	private fun prepareMenu(menu: Menu) {
		MenuCompat.setGroupDividerEnabled(menu, true)
		menu.populateBranchList()
		val history = viewModel.historyInfo.value
		menu.findItem(R.id.action_incognito)?.isVisible = !history.isIncognitoMode
		menu.findItem(R.id.action_forget)?.isVisible = history.history != null
		menu.findItem(R.id.action_download)?.isVisible = viewModel.getMangaOrNull()?.isLocal == false
	}

	private fun openReader(isIncognitoMode: Boolean) {
		val manga = viewModel.getMangaOrNull() ?: return
		if (viewModel.historyInfo.value.isChapterMissing) {
			Snackbar.make(buttonRead, R.string.chapter_is_missing, Snackbar.LENGTH_SHORT)
				.show() // TODO
		} else {
			val intentBuilder = ReaderIntent.Builder(context)
				.manga(manga)
				.branch(viewModel.selectedBranchValue)
			if (isIncognitoMode) {
				intentBuilder.incognito()
			}
			router.openReader(intentBuilder.build())
			if (isIncognitoMode) {
				Toast.makeText(context, R.string.incognito_mode, Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun onHistoryChanged(isLoading: Boolean, info: HistoryInfo) {
		val isChaptersLoading = isLoading && (info.totalChapters <= 0 || info.isChapterMissing)
		buttonRead.setText(
			when {
				isChaptersLoading -> R.string.loading_
				info.isIncognitoMode -> R.string.incognito
				info.canContinue -> R.string._continue
				else -> R.string.read
			},
		)
		splitButton.isEnabled = !isChaptersLoading && info.isValid
	}

	private fun Menu.populateBranchList() {
		val branches = viewModel.branches.value
		if (branches.size <= 1) {
			return
		}
		for ((i, branch) in branches.withIndex()) {
			val title = buildSpannedString {
				if (branch.isCurrent) {
					inSpans(
						ImageSpan(
							context,
							R.drawable.ic_current_chapter,
							DynamicDrawableSpan.ALIGN_BASELINE,
						),
					) {
						append(' ')
					}
					append(' ')
				}
				append(branch.name ?: context.getString(R.string.system_default))
				append(' ')
				append(' ')
				inSpans(
					ForegroundColorSpan(
						context.getThemeColor(
							android.R.attr.textColorSecondary,
							Color.LTGRAY,
						),
					),
					RelativeSizeSpan(0.74f),
				) {
					append(branch.count.toString())
				}
			}
			val item = add(R.id.group_branches, Menu.NONE, i, title)
			item.isCheckable = true
			item.isChecked = branch.isSelected
		}
		setGroupCheckable(R.id.group_branches, true, true)
	}
}
