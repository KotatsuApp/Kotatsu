package org.koitharu.kotatsu.core.ui.dialog

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.databinding.DialogTwoButtonsBinding

class BigButtonsAlertDialog private constructor(
	private val delegate: AlertDialog
) : DialogInterface by delegate {

	fun show() = delegate.show()

	class Builder(context: Context) {

		private val binding = DialogTwoButtonsBinding.inflate(LayoutInflater.from(context))

		private val delegate = MaterialAlertDialogBuilder(context)
			.setView(binding.root)

		fun setTitle(@StringRes titleResId: Int): Builder {
			binding.title.setText(titleResId)
			return this
		}

		fun setTitle(title: CharSequence): Builder {
			binding.title.text = title
			return this
		}

		fun setIcon(@DrawableRes iconId: Int): Builder {
			binding.icon.setImageResource(iconId)
			return this
		}

		fun setPositiveButton(
			@StringRes textId: Int,
			listener: DialogInterface.OnClickListener,
		): Builder {
			initButton(binding.button1, DialogInterface.BUTTON_POSITIVE, textId, listener)
			return this
		}

		fun setNegativeButton(
			@StringRes textId: Int,
			listener: DialogInterface.OnClickListener? = null
		): Builder {
			initButton(binding.button3, DialogInterface.BUTTON_NEGATIVE, textId, listener)
			return this
		}

		fun setNeutralButton(
			@StringRes textId: Int,
			listener: DialogInterface.OnClickListener? = null
		): Builder {
			initButton(binding.button2, DialogInterface.BUTTON_NEUTRAL, textId, listener)
			return this
		}

		fun create(): BigButtonsAlertDialog {
			with(binding) {
				button1.adjustCorners(isFirst = true, isLast = button2.isGone && button3.isGone)
				button2.adjustCorners(isFirst = button1.isGone, isLast = button3.isGone)
				button3.adjustCorners(isFirst = button1.isGone && button2.isGone, isLast = true)
			}

			val dialog = delegate.create()
			binding.root.tag = dialog
			return BigButtonsAlertDialog(dialog)
		}

		private fun MaterialButton.adjustCorners(isFirst: Boolean, isLast: Boolean) {
			if (!isVisible) {
				return
			}
			shapeAppearanceModel = shapeAppearanceModel.toBuilder().apply {
				if (!isFirst) {
					setTopLeftCornerSize(0f)
					setTopRightCornerSize(0f)
				}
				if (!isLast) {
					setBottomLeftCornerSize(0f)
					setBottomRightCornerSize(0f)
				}
			}.build()
		}

		private fun initButton(
			button: MaterialButton,
			which: Int,
			@StringRes textId: Int,
			listener: DialogInterface.OnClickListener?,
		) {
			button.setText(textId)
			button.isVisible = true
			button.setOnClickListener {
				val dialog = binding.root.tag as DialogInterface
				listener?.onClick(dialog, which)
				dialog.dismiss()
			}
		}
	}
}
