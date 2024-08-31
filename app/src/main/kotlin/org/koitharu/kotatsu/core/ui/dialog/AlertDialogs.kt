package org.koitharu.kotatsu.core.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.widget.CompoundButton.OnCheckedChangeListener
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
import org.koitharu.kotatsu.databinding.DialogCheckboxBinding
import com.google.android.material.R as materialR

inline fun buildAlertDialog(
	context: Context,
	isCentered: Boolean = false,
	block: MaterialAlertDialogBuilder.() -> Unit,
): AlertDialog = MaterialAlertDialogBuilder(
	context,
	if (isCentered) materialR.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered else 0,
).apply(block).create()

fun <B : AlertDialog.Builder> B.setCheckbox(
	@StringRes textResId: Int,
	isChecked: Boolean,
	onCheckedChangeListener: OnCheckedChangeListener
) = apply {
	val binding = DialogCheckboxBinding.inflate(LayoutInflater.from(context))
	binding.checkbox.setText(textResId)
	binding.checkbox.isChecked = isChecked
	binding.checkbox.setOnCheckedChangeListener(onCheckedChangeListener)
	setView(binding.root)
}

fun <B : AlertDialog.Builder, T> B.setRecyclerViewList(
	list: List<T>,
	delegate: AdapterDelegate<List<T>>,
) = apply {
	val delegatesManager = AdapterDelegatesManager<List<T>>()
	delegatesManager.addDelegate(delegate)
	setRecyclerViewList(ListDelegationAdapter(delegatesManager).also { it.items = list })
}

fun <B : AlertDialog.Builder, T> B.setRecyclerViewList(
	list: List<T>,
	vararg delegates: AdapterDelegate<List<T>>,
) = apply {
	val delegatesManager = AdapterDelegatesManager<List<T>>()
	delegates.forEach { delegatesManager.addDelegate(it) }
	setRecyclerViewList(ListDelegationAdapter(delegatesManager).also { it.items = list })
}

fun <B : AlertDialog.Builder> B.setRecyclerViewList(adapter: RecyclerView.Adapter<*>) = apply {
	val recyclerView = RecyclerView(context)
	recyclerView.layoutManager = LinearLayoutManager(context)
	recyclerView.updatePadding(
		top = context.resources.getDimensionPixelOffset(R.dimen.list_spacing),
	)
	recyclerView.clipToPadding = false
	recyclerView.adapter = adapter
	setView(recyclerView)
}
