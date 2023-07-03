package org.koitharu.kotatsu.core.ui

import androidx.recyclerview.widget.AsyncListDiffer.ListListener
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import kotlinx.coroutines.flow.FlowCollector
import org.koitharu.kotatsu.core.util.ContinuationResumeRunnable
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel
import kotlin.coroutines.suspendCoroutine

abstract class BaseListAdapter : AsyncListDifferDelegationAdapter<ListModel>(ListModelDiffCallback),
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
