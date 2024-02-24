package org.koitharu.kotatsu.core.ui.dialog

import android.content.Context
import android.content.DialogInterface
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.hannesdorfmann.adapterdelegates4.AdapterDelegatesManager
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import org.koitharu.kotatsu.R

class RecyclerViewAlertDialog private constructor(
	private val delegate: AlertDialog
) : DialogInterface by delegate {

	fun show() = delegate.show()

	class Builder<T>(context: Context) {

		private val recyclerView = RecyclerView(context)
		private val delegatesManager = AdapterDelegatesManager<List<T>>()
		private var items: List<T>? = null

		private val delegate = MaterialAlertDialogBuilder(context)
			.setView(recyclerView)

		init {
			recyclerView.layoutManager = LinearLayoutManager(context)
			recyclerView.updatePadding(
				top = context.resources.getDimensionPixelOffset(R.dimen.list_spacing),
			)
			recyclerView.clipToPadding = false
		}

		fun setTitle(@StringRes titleResId: Int): Builder<T> {
			delegate.setTitle(titleResId)
			return this
		}

		fun setTitle(title: CharSequence): Builder<T> {
			delegate.setTitle(title)
			return this
		}

		fun setIcon(@DrawableRes iconId: Int): Builder<T> {
			delegate.setIcon(iconId)
			return this
		}

		fun setPositiveButton(
			@StringRes textId: Int,
			listener: DialogInterface.OnClickListener,
		): Builder<T> {
			delegate.setPositiveButton(textId, listener)
			return this
		}

		fun setNegativeButton(
			@StringRes textId: Int,
			listener: DialogInterface.OnClickListener? = null
		): Builder<T> {
			delegate.setNegativeButton(textId, listener)
			return this
		}

		fun setNeutralButton(
			@StringRes textId: Int,
			listener: DialogInterface.OnClickListener,
		): Builder<T> {
			delegate.setNeutralButton(textId, listener)
			return this
		}

		fun setCancelable(isCancelable: Boolean): Builder<T> {
			delegate.setCancelable(isCancelable)
			return this
		}

		fun addAdapterDelegate(subject: AdapterDelegate<List<T>>): Builder<T> {
			delegatesManager.addDelegate(subject)
			return this
		}

		fun setItems(list: List<T>): Builder<T> {
			items = list
			return this
		}

		fun create(): RecyclerViewAlertDialog {
			recyclerView.adapter = ListDelegationAdapter(delegatesManager).also {
				it.items = items
			}
			return RecyclerViewAlertDialog(delegate.create())
		}
	}
}
