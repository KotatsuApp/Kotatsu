package org.koitharu.kotatsu.core.ui.list

import android.app.Activity
import android.os.Bundle
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
import org.koitharu.kotatsu.core.ui.list.decor.AbstractSelectionItemDecoration
import kotlin.coroutines.EmptyCoroutineContext

private const val KEY_SELECTION = "selection"
private const val PROVIDER_NAME = "selection_decoration"

class ListSelectionController(
	private val activity: Activity,
	private val decoration: AbstractSelectionItemDecoration,
	private val registryOwner: SavedStateRegistryOwner,
	private val callback: Callback2,
) : ActionMode.Callback, SavedStateRegistry.SavedStateProvider {

	private var actionMode: ActionMode? = null

	val count: Int
		get() = decoration.checkedItemsCount

	init {
		registryOwner.lifecycle.addObserver(StateEventObserver())
	}

	fun snapshot(): Set<Long> {
		return peekCheckedIds().toSet()
	}

	fun peekCheckedIds(): Set<Long> {
		return decoration.checkedItemsIds
	}

	fun clear() {
		decoration.clearSelection()
		notifySelectionChanged()
	}

	fun addAll(ids: Collection<Long>) {
		if (ids.isEmpty()) {
			return
		}
		decoration.checkAll(ids)
		notifySelectionChanged()
	}

	fun attachToRecyclerView(recyclerView: RecyclerView) {
		recyclerView.addItemDecoration(decoration)
	}

	override fun saveState(): Bundle {
		val bundle = Bundle(1)
		bundle.putLongArray(KEY_SELECTION, peekCheckedIds().toLongArray())
		return bundle
	}

	fun onItemClick(id: Long): Boolean {
		if (decoration.checkedItemsCount != 0) {
			decoration.toggleItemChecked(id)
			if (decoration.checkedItemsCount == 0) {
				actionMode?.finish()
			} else {
				actionMode?.invalidate()
			}
			notifySelectionChanged()
			return true
		}
		return false
	}

	fun onItemLongClick(id: Long): Boolean {
		startActionMode()
		return actionMode?.also {
			decoration.setItemIsChecked(id, true)
			notifySelectionChanged()
		} != null
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

	private fun notifySelectionChanged() {
		val count = decoration.checkedItemsCount
		callback.onSelectionChanged(this, count)
		if (count == 0) {
			actionMode?.finish()
		} else {
			actionMode?.invalidate()
		}
	}

	private fun restoreState(ids: Collection<Long>) {
		if (ids.isEmpty() || decoration.checkedItemsCount != 0) {
			return
		}
		decoration.checkAll(ids)
		startActionMode()
		notifySelectionChanged()
	}

	@Deprecated("")
	interface Callback : Callback2 {

		fun onSelectionChanged(count: Int)

		fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean

		fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean

		fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean

		fun onDestroyActionMode(mode: ActionMode) = Unit

		override fun onSelectionChanged(controller: ListSelectionController, count: Int) {
			onSelectionChanged(count)
		}

		override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
			return onCreateActionMode(mode, menu)
		}

		override fun onPrepareActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
			return onPrepareActionMode(mode, menu)
		}

		override fun onActionItemClicked(
			controller: ListSelectionController,
			mode: ActionMode,
			item: MenuItem,
		): Boolean = onActionItemClicked(mode, item)

		override fun onDestroyActionMode(controller: ListSelectionController, mode: ActionMode) {
			onDestroyActionMode(mode)
		}
	}

	interface Callback2 {

		fun onSelectionChanged(controller: ListSelectionController, count: Int)

		fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean

		fun onPrepareActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
			mode.title = controller.count.toString()
			return true
		}

		fun onActionItemClicked(controller: ListSelectionController, mode: ActionMode, item: MenuItem): Boolean

		fun onDestroyActionMode(controller: ListSelectionController, mode: ActionMode) = Unit
	}

	private inner class StateEventObserver : LifecycleEventObserver {

		override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
			if (event == Lifecycle.Event.ON_CREATE) {
				val registry = registryOwner.savedStateRegistry
				registry.registerSavedStateProvider(PROVIDER_NAME, this@ListSelectionController)
				val state = registry.consumeRestoredStateForKey(PROVIDER_NAME)
				if (state != null) {
					Dispatchers.Main.dispatch(EmptyCoroutineContext) { // == Handler.post
						if (source.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
							restoreState(state.getLongArray(KEY_SELECTION)?.toList().orEmpty())
						}
					}
				}
			}
		}
	}
}
