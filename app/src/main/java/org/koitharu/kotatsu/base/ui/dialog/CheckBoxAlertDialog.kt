package org.koitharu.kotatsu.base.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.checkbox.MaterialCheckBox
import org.koitharu.kotatsu.R

class CheckBoxAlertDialog private constructor(private val delegate: AlertDialog) :
	DialogInterface by delegate {

	fun show() = delegate.show()

	class Builder(context: Context) {

		@SuppressLint("InflateParams")
		private val view = LayoutInflater.from(context)
			.inflate(R.layout.dialog_checkbox, null, false)
		private val checkBox = view.findViewById<MaterialCheckBox>(android.R.id.checkbox)

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

		fun setMessage(@StringRes messageId: Int): Builder {
			delegate.setMessage(messageId)
			return this
		}

		fun setMessage(message: CharSequence): Builder {
			delegate.setMessage(message)
			return this
		}

		fun setCheckBoxText(@StringRes textId: Int): Builder {
			checkBox.setText(textId)
			return this
		}

		fun setCheckBoxChecked(isChecked: Boolean): Builder {
			checkBox.isChecked = isChecked
			return this
		}

		fun setIcon(@DrawableRes iconId: Int): Builder {
			delegate.setIcon(iconId)
			return this
		}

		fun setPositiveButton(
			@StringRes textId: Int,
			listener: (DialogInterface, Boolean) -> Unit
		): Builder {
			delegate.setPositiveButton(textId) { dialog, _ ->
				listener(dialog, checkBox.isChecked)
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

		fun create() = CheckBoxAlertDialog(delegate.create())

	}
}