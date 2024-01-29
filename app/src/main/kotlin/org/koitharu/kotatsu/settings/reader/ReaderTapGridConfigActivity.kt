package org.koitharu.kotatsu.settings.reader

import android.content.DialogInterface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.Insets
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.updatePadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.ext.findKeyByValue
import org.koitharu.kotatsu.core.util.ext.getThemeDrawable
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.databinding.ActivityReaderTapActionsBinding
import org.koitharu.kotatsu.reader.data.TapGridSettings
import org.koitharu.kotatsu.reader.domain.TapGridArea
import org.koitharu.kotatsu.reader.ui.tapgrid.TapAction
import java.util.EnumMap
import javax.inject.Inject
import com.google.android.material.R as materialR

@AndroidEntryPoint
class ReaderTapGridConfigActivity : BaseActivity<ActivityReaderTapActionsBinding>(), View.OnClickListener,
	View.OnLongClickListener {

	@Inject
	lateinit var tapGridSettings: TapGridSettings

	private val controls = EnumMap<TapGridArea, TextView>(TapGridArea::class.java)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityReaderTapActionsBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		controls[TapGridArea.TOP_LEFT] = viewBinding.textViewTopLeft
		controls[TapGridArea.TOP_CENTER] = viewBinding.textViewTopCenter
		controls[TapGridArea.TOP_RIGHT] = viewBinding.textViewTopRight
		controls[TapGridArea.CENTER_LEFT] = viewBinding.textViewCenterLeft
		controls[TapGridArea.CENTER] = viewBinding.textViewCenter
		controls[TapGridArea.CENTER_RIGHT] = viewBinding.textViewCenterRight
		controls[TapGridArea.BOTTOM_LEFT] = viewBinding.textViewBottomLeft
		controls[TapGridArea.BOTTOM_CENTER] = viewBinding.textViewBottomCenter
		controls[TapGridArea.BOTTOM_RIGHT] = viewBinding.textViewBottomRight

		controls.forEach { (_, view) ->
			view.setOnClickListener(this)
			view.setOnLongClickListener(this)
		}
		updateValues()
		tapGridSettings.observe().observe(this) { updateValues() }
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.opt_tap_grid_config, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_reset -> {
				confirmReset()
				true
			}

			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			top = insets.top,
			right = insets.right,
			bottom = insets.bottom,
		)
	}

	override fun onClick(v: View) {
		val area = controls.findKeyByValue(v) ?: return
		showActionSelector(area, isLongTap = false)
	}

	override fun onLongClick(v: View?): Boolean {
		val area = controls.findKeyByValue(v) ?: return false
		showActionSelector(area, isLongTap = true)
		return true
	}

	private fun updateValues() {
		controls.forEach { (area, view) ->
			view.text = buildSpannedString {
				appendLine(getString(R.string.tap_action))
				bold {
					appendLine(getTapActionText(area, isLongTap = false))
				}
				appendLine()
				appendLine(getString(R.string.long_tap_action))
				bold {
					appendLine(getTapActionText(area, isLongTap = true))
				}
			}
			view.background = createBackground(tapGridSettings.getTapAction(area, false))
		}
	}

	private fun getTapActionText(area: TapGridArea, isLongTap: Boolean): String {
		return tapGridSettings.getTapAction(area, isLongTap)?.let {
			getString(it.nameStringResId)
		} ?: getString(R.string.none)
	}

	private fun showActionSelector(area: TapGridArea, isLongTap: Boolean) {
		val selectedItem = tapGridSettings.getTapAction(area, isLongTap)?.ordinal ?: -1
		val listener = DialogInterface.OnClickListener { dialog, which ->
			tapGridSettings.setTapAction(area, isLongTap, TapAction.entries.getOrNull(which - 1))
			dialog.dismiss()
		}
		val names = arrayOfNulls<String>(TapAction.entries.size + 1)
		names[0] = getString(R.string.none)
		TapAction.entries.forEachIndexed { index, action -> names[index + 1] = getString(action.nameStringResId) }
		MaterialAlertDialogBuilder(this)
			.setSingleChoiceItems(names, selectedItem + 1, listener)
			.setTitle(if (isLongTap) R.string.long_tap_action else R.string.tap_action)
			.setIcon(R.drawable.ic_tap)
			.setNegativeButton(android.R.string.cancel, null)
			.show()
	}

	private fun confirmReset() {
		MaterialAlertDialogBuilder(this)
			.setMessage(R.string.config_reset_confirm)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.reset) { _, _ ->
				tapGridSettings.reset()
			}.show()
	}

	private fun createBackground(action: TapAction?): Drawable? {
		val ripple = getThemeDrawable(materialR.attr.selectableItemBackground)
		return if (action == null) {
			ripple
		} else {
			LayerDrawable(arrayOf(ripple, ColorDrawable(ColorUtils.setAlphaComponent(action.color, 40))))
		}
	}
}
