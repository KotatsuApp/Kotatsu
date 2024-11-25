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
import androidx.core.view.get
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialSplitButton
import com.google.android.material.snackbar.Snackbar
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.details.ui.model.HistoryInfo
import org.koitharu.kotatsu.reader.ui.ReaderActivity

class ReadButtonDelegate(
	splitButton: MaterialSplitButton,
	private val viewModel: DetailsViewModel,
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
			else -> {
				val branch = viewModel.branches.value.getOrNull(item.order) ?: return false
				viewModel.setSelectedBranch(branch.name)
			}
		}
		return true
	}

	override fun onDismiss(menu: PopupMenu?) {
		buttonMenu.isChecked = false
	}

	fun attach(lifecycleOwner: LifecycleOwner) {
		buttonRead.setOnClickListener(this)
		buttonMenu.setOnClickListener(this)
		viewModel.historyInfo.observe(lifecycleOwner, this::onHistoryChanged)
	}

	private fun showMenu() {
		val menu = PopupMenu(context, buttonMenu)
		menu.inflate(R.menu.popup_read)
		menu.menu.setGroupDividerEnabled(true)
		menu.menu.populateBranchList()
		menu.menu.findItem(R.id.action_forget)?.isVisible = viewModel.historyInfo.value.run {
			!isIncognitoMode && history != null
		}
		menu.setOnMenuItemClickListener(this)
		menu.setForceShowIcon(true)
		menu.setOnDismissListener(this)
		buttonMenu.isChecked = true
		menu.show()
	}

	private fun openReader(isIncognitoMode: Boolean) {
		val detailsViewModel = viewModel as? DetailsViewModel ?: return
		val manga = viewModel.manga.value ?: return
		if (detailsViewModel.historyInfo.value.isChapterMissing) {
			Snackbar.make(buttonRead, R.string.chapter_is_missing, Snackbar.LENGTH_SHORT)
				.show() // TODO
		} else {
			context.startActivity(
				ReaderActivity.IntentBuilder(context)
					.manga(manga)
					.branch(detailsViewModel.selectedBranchValue)
					.incognito(isIncognitoMode)
					.build(),
			)
			if (isIncognitoMode) {
				Toast.makeText(context, R.string.incognito_mode, Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun onHistoryChanged(info: HistoryInfo) {
		buttonRead.setText(if (info.canContinue) R.string._continue else R.string.read)
		buttonRead.isEnabled = info.isValid
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
