package org.koitharu.kotatsu.core.ui.dialog

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.databinding.DialogTwoButtonsBinding

class TwoButtonsAlertDialog private constructor(
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
			initButton(binding.button2, DialogInterface.BUTTON_NEGATIVE, textId, listener)
			return this
		}

		fun create(): TwoButtonsAlertDialog {
			val dialog = delegate.create()
			binding.root.tag = dialog
			return TwoButtonsAlertDialog(dialog)
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
