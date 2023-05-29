package org.koitharu.kotatsu.core.ui.list

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.collection.ArrayMap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.Dispatchers
import org.koitharu.kotatsu.core.ui.list.decor.AbstractSelectionItemDecoration
import kotlin.coroutines.EmptyCoroutineContext

private const val PROVIDER_NAME = "selection_decoration_sectioned"

class SectionedSelectionController<T : Any>(
	private val activity: Activity,
	private val owner: SavedStateRegistryOwner,
	private val callback: Callback<T>,
) : ActionMode.Callback, SavedStateRegistry.SavedStateProvider {

	private var actionMode: ActionMode? = null

	private var pendingData: MutableMap<String, Collection<Long>>? = null
	private val decorations = ArrayMap<T, AbstractSelectionItemDecoration>()

	val count: Int
		get() = decorations.values.sumOf { it.checkedItemsCount }

	init {
		owner.lifecycle.addObserver(StateEventObserver())
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
		var shouldAddDecoration = true
		for (i in (0 until recyclerView.itemDecorationCount).reversed()) {
			val decor = recyclerView.getItemDecorationAt(i)
			if (decor === decoration) {
				shouldAddDecoration = false
				break
			} else if (decor.javaClass == decoration.javaClass) {
				recyclerView.removeItemDecorationAt(i)
			}
		}
		if (shouldAddDecoration) {
			recyclerView.addItemDecoration(decoration)
		}
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

	fun getSectionCount(section: T): Int {
		return decorations[section]?.checkedItemsCount ?: 0
	}

	fun addToSelection(section: T, ids: Collection<Long>): Boolean {
		val decoration = getDecoration(section)
		startActionMode()
		return actionMode?.also {
			decoration.checkAll(ids)
			notifySelectionChanged()
		} != null
	}

	fun clearSelection(section: T) {
		decorations[section]?.clearSelection() ?: return
		notifySelectionChanged()
	}

	override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
		return callback.onCreateActionMode(this, mode, menu)
	}

	override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
		return callback.onPrepareActionMode(this, mode, menu)
	}

	override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
		return callback.onActionItemClicked(this, mode, item)
	}

	override fun onDestroyActionMode(mode: ActionMode) {
		callback.onDestroyActionMode(this, mode)
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
		callback.onSelectionChanged(this, count)
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
			callback.onCreateItemDecoration(this, section)
		}
	}

	interface Callback<T : Any> {

		fun onSelectionChanged(controller: SectionedSelectionController<T>, count: Int)

		fun onCreateActionMode(controller: SectionedSelectionController<T>, mode: ActionMode, menu: Menu): Boolean

		fun onPrepareActionMode(controller: SectionedSelectionController<T>, mode: ActionMode, menu: Menu): Boolean {
			mode.title = controller.count.toString()
			return true
		}

		fun onDestroyActionMode(controller: SectionedSelectionController<T>, mode: ActionMode) = Unit

		fun onActionItemClicked(
			controller: SectionedSelectionController<T>,
			mode: ActionMode,
			item: MenuItem,
		): Boolean

		fun onCreateItemDecoration(
			controller: SectionedSelectionController<T>,
			section: T,
		): AbstractSelectionItemDecoration
	}

	private inner class StateEventObserver : LifecycleEventObserver {

		override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
			if (event == Lifecycle.Event.ON_CREATE) {
				val registry = owner.savedStateRegistry
				registry.registerSavedStateProvider(PROVIDER_NAME, this@SectionedSelectionController)
				val state = registry.consumeRestoredStateForKey(PROVIDER_NAME)
				if (state != null) {
					Dispatchers.Main.dispatch(EmptyCoroutineContext) { // == Handler.post
						if (source.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
							restoreState(
								state.keySet()
									.associateWithTo(HashMap()) { state.getLongArray(it)?.toList().orEmpty() },
							)
						}
					}
				}
			}
		}
	}
}
