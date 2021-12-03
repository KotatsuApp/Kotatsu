package org.koitharu.kotatsu.base.ui.dialog

import android.content.Context
import android.content.DialogInterface
import android.text.InputFilter
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import org.koitharu.kotatsu.databinding.DialogInputBinding

class TextInputDialog private constructor(
	private val delegate: AlertDialog,
) : DialogInterface by delegate {

	fun show() = delegate.show()

	class Builder(context: Context) {

		private val binding = DialogInputBinding.inflate(LayoutInflater.from(context))

		private val delegate = AlertDialog.Builder(context)
			.setView(binding.root)

		fun setTitle(@StringRes titleResId: Int): Builder {
			delegate.setTitle(titleResId)
			return this
		}

		fun setTitle(title: CharSequence): Builder {
			delegate.setTitle(title)
			return this
		}

		fun setHint(@StringRes hintResId: Int): Builder {
			binding.inputEdit.hint = binding.root.context.getString(hintResId)
			return this
		}

		fun setMaxLength(maxLength: Int, strict: Boolean): Builder {
			with(binding.inputLayout) {
				counterMaxLength = maxLength
				isCounterEnabled = maxLength > 0
			}
			if (strict && maxLength > 0) {
				binding.inputEdit.filters += InputFilter.LengthFilter(maxLength)
			}
			return this
		}

		fun setInputType(inputType: Int): Builder {
			binding.inputEdit.inputType = inputType
			return this
		}

		fun setText(text: String): Builder {
			binding.inputEdit.setText(text)
			binding.inputEdit.setSelection(text.length)
			return this
		}

		fun setPositiveButton(
			@StringRes textId: Int,
			listener: (DialogInterface, String) -> Unit
		): Builder {
			delegate.setPositiveButton(textId) { dialog, _ ->
				listener(dialog, binding.inputEdit.text?.toString().orEmpty())
			}
			return this
		}

		fun setNegativeButton(
			@StringRes textId: Int,
			listener: DialogInterface.OnClickListener? = null
		): Builder {
			delegate.setNegativeButton(textId, listener)
			return this
		}

		fun setOnCancelListener(listener: DialogInterface.OnCancelListener): Builder {
			delegate.setOnCancelListener(listener)
			return this
		}

		fun create() =
			TextInputDialog(delegate.create())

	}
}