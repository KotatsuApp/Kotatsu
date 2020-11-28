package org.koitharu.kotatsu.base.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.text.InputFilter
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.dialog_input.view.*
import org.koitharu.kotatsu.R

class TextInputDialog private constructor(
	private val delegate: AlertDialog
) : DialogInterface by delegate {

	fun show() = delegate.show()

	class Builder(context: Context) {

		@SuppressLint("InflateParams")
		private val view = LayoutInflater.from(context)
			.inflate(R.layout.dialog_input, null, false)

		private val delegate = AlertDialog.Builder(context)
			.setView(view)

		fun setTitle(@StringRes titleResId: Int): Builder {
			delegate.setTitle(titleResId)
			return this
		}

		fun setTitle(title: CharSequence): Builder {
			delegate.setTitle(title)
			return this
		}

		fun setHint(@StringRes hintResId: Int): Builder {
			view.inputLayout.hint = view.context.getString(hintResId)
			return this
		}

		fun setMaxLength(maxLength: Int, strict: Boolean): Builder {
			with(view.inputLayout) {
				counterMaxLength = maxLength
				isCounterEnabled = maxLength > 0
			}
			if (strict && maxLength > 0) {
				view.inputEdit.filters += InputFilter.LengthFilter(maxLength)
			}
			return this
		}

		fun setInputType(inputType: Int): Builder {
			view.inputEdit.inputType = inputType
			return this
		}

		fun setText(text: String): Builder {
			view.inputEdit.setText(text)
			view.inputEdit.setSelection(text.length)
			return this
		}

		fun setPositiveButton(
			@StringRes textId: Int,
			listener: (DialogInterface, String) -> Unit
		): Builder {
			delegate.setPositiveButton(textId) { dialog, _ ->
				listener(dialog, view.inputEdit.text?.toString().orEmpty())
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