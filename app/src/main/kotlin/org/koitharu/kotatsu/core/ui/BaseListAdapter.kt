package org.koitharu.kotatsu.core.ui

import androidx.recyclerview.widget.AsyncListDiffer.ListListener
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import kotlinx.coroutines.flow.FlowCollector
import org.koitharu.kotatsu.core.util.ContinuationResumeRunnable
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel
import kotlin.coroutines.suspendCoroutine

open class BaseListAdapter(
	vararg delegates: AdapterDelegate<List<ListModel>>,
) : AsyncListDifferDelegationAdapter<ListModel>(ListModelDiffCallback, *delegates),
	FlowCollector<List<ListModel>> {

	override suspend fun emit(value: List<ListModel>) = suspendCoroutine { cont ->
		setItems(value, ContinuationResumeRunnable(cont))
	}

	fun addListListener(listListener: ListListener<ListModel>) {
		differ.addListListener(listListener)
	}

	fun removeListListener(listListener: ListListener<ListModel>) {
		differ.removeListListener(listListener)
	}
}
