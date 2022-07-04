package org.koitharu.kotatsu.base.ui.list

import android.app.Activity
import android.os.Bundle
import android.util.ArrayMap
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.Dispatchers
import org.koitharu.kotatsu.base.ui.list.decor.AbstractSelectionItemDecoration
import kotlin.coroutines.EmptyCoroutineContext

private const val PROVIDER_NAME = "selection_decoration_sectioned"

class SectionedSelectionController<T : Any>(
	private val activity: Activity,
	private val registryOwner: SavedStateRegistryOwner,
	private val callback: Callback<T>,
) : ActionMode.Callback, SavedStateRegistry.SavedStateProvider {

	private var actionMode: ActionMode? = null

	private var pendingData: MutableMap<String, Collection<Long>>? = null
	private val decorations = ArrayMap<T, AbstractSelectionItemDecoration>()

	val count: Int
		get() = decorations.values.sumOf { it.checkedItemsCount }

	init {
		registryOwner.lifecycle.addObserver(StateEventObserver())
	}

	fun snapshot(): Map<T, Set<Long>> {
		return decorations.mapValues { it.value.checkedItemsIds.toSet() }
	}

	fun peekCheckedIds(): Map<T, Set<Long>> {
		return decorations.mapValues { it.value.checkedItemsIds }
	}

	fun clear() {
		decorations.values.forEach {
			it.clearSelection()
		}
		notifySelectionChanged()
	}

	fun attachToRecyclerView(section: T, recyclerView: RecyclerView) {
		val decoration = getDecoration(section)
		val pendingIds = pendingData?.remove(section.toString())
		if (!pendingIds.isNullOrEmpty()) {
			decoration.checkAll(pendingIds)
			startActionMode()
			notifySelectionChanged()
		}
		recyclerView.addItemDecoration(decoration)
		if (pendingData?.isEmpty() == true) {
			pendingData = null
		}
	}

	override fun saveState(): Bundle {
		val bundle = Bundle(decorations.size)
		for ((k, v) in decorations) {
			bundle.putLongArray(k.toString(), v.checkedItemsIds.toLongArray())
		}
		return bundle
	}

	fun onItemClick(section: T, id: Long): Boolean {
		val decoration = getDecoration(section)
		if (isInSelectionMode()) {
			decoration.toggleItemChecked(id)
			if (isInSelectionMode()) {
				actionMode?.invalidate()
			} else {
				actionMode?.finish()
			}
			notifySelectionChanged()
			return true
		}
		return false
	}

	fun onItemLongClick(section: T, id: Long): Boolean {
		val decoration = getDecoration(section)
		startActionMode()
		return actionMode?.also {
			decoration.setItemIsChecked(id, true)
			notifySelectionChanged()
		} != null
	}

	override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
		return callback.onCreateActionMode(mode, menu)
	}

	override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
		return callback.onPrepareActionMode(mode, menu)
	}

	override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
		return callback.onActionItemClicked(mode, item)
	}

	override fun onDestroyActionMode(mode: ActionMode) {
		callback.onDestroyActionMode(mode)
		clear()
		actionMode = null
	}

	private fun startActionMode() {
		if (actionMode == null) {
			actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(this)
		}
	}

	private fun isInSelectionMode(): Boolean {
		return decorations.values.any { x -> x.checkedItemsCount > 0 }
	}

	private fun notifySelectionChanged() {
		val count = this.count
		callback.onSelectionChanged(count)
		if (count == 0) {
			actionMode?.finish()
		} else {
			actionMode?.invalidate()
		}
	}

	private fun restoreState(ids: MutableMap<String, Collection<Long>>) {
		if (ids.isEmpty() || isInSelectionMode()) {
			return
		}
		for ((k, v) in decorations) {
			val items = ids.remove(k.toString())
			if (!items.isNullOrEmpty()) {
				v.checkAll(items)
			}
		}
		pendingData = ids
		if (isInSelectionMode()) {
			startActionMode()
			notifySelectionChanged()
		}
	}

	private fun getDecoration(section: T): AbstractSelectionItemDecoration {
		return decorations.getOrPut(section) {
			callback.onCreateItemDecoration(section)
		}
	}

	interface Callback<T> : ListSelectionController.Callback {

		fun onCreateItemDecoration(section: T): AbstractSelectionItemDecoration
	}

	private inner class StateEventObserver : LifecycleEventObserver {

		override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
			if (event == Lifecycle.Event.ON_CREATE) {
				val registry = registryOwner.savedStateRegistry
				registry.registerSavedStateProvider(PROVIDER_NAME, this@SectionedSelectionController)
				val state = registry.consumeRestoredStateForKey(PROVIDER_NAME)
				if (state != null) {
					Dispatchers.Main.dispatch(EmptyCoroutineContext) { // == Handler.post
						if (source.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
							restoreState(
								state.keySet().associateWithTo(HashMap()) { state.getLongArray(it)?.toList().orEmpty() }
							)
						}
					}
				}
			}
		}
	}
}