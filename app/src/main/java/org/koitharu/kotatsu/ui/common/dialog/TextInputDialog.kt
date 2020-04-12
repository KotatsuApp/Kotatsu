package org.koitharu.kotatsu.ui.common.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.dialog_input.view.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.utils.ext.showKeyboard

class TextInputDialog private constructor(private val delegate: AlertDialog) :
	DialogInterface by delegate {

	init {
		delegate.setOnShowListener {
			val view = delegate.findViewById<TextView>(R.id.inputEdit)?:return@setOnShowListener
			view.post {
				view.requestFocus()
				view.showKeyboard()
			}
		}
	}

	fun show() = delegate.show()

	class Builder(context: Context) {

		@SuppressLint("InflateParams")
		private val view = LayoutInflater.from(context).inflate(R.layout.dialog_input, null, false)

		private val delegate = AlertDialog.Builder(context)
			.setView(view)
			.setOnDismissListener {
				val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
				imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
			}

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

		fun setInputType(inputType: Int): Builder {
			view.inputEdit.inputType = inputType
			return this
		}

		fun setText(text: String): Builder {
			view.inputEdit.setText(text)
			view.inputEdit.setSelection(text.length)
			return this
		}

		fun setPositiveButton(@StringRes textId: Int, listener: (DialogInterface, String) -> Unit): Builder {
			delegate.setPositiveButton(textId) { dialog, _ ->
				listener(dialog, view.inputEdit.text?.toString().orEmpty())
			}
			return this
		}

		fun setNegativeButton(@StringRes textId: Int, listener: DialogInterface.OnClickListener? = null): Builder {
			delegate.setNegativeButton(textId, listener)
			return this
		}

		fun create() =
			TextInputDialog(delegate.create())

	}
}