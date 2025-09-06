package org.koitharu.kotatsu.core.ui.list

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.PopupMenu
import androidx.collection.LongSet
import androidx.collection.longSetOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.Dispatchers
import org.koitharu.kotatsu.core.ui.list.decor.AbstractSelectionItemDecoration
import org.koitharu.kotatsu.core.util.ext.toLongArray
import org.koitharu.kotatsu.core.util.ext.toSet
import kotlin.coroutines.EmptyCoroutineContext

private const val KEY_SELECTION = "selection"
private const val PROVIDER_NAME = "selection_decoration"

class ListSelectionController(
	private val appCompatDelegate: AppCompatDelegate,
	private val decoration: AbstractSelectionItemDecoration,
	private val registryOwner: SavedStateRegistryOwner,
	private val callback: Callback,
) : ActionMode.Callback, SavedStateRegistry.SavedStateProvider {

	private var actionMode: ActionMode? = null
	private var focusedItemId: LongSet? = null

	var useActionMode: Boolean = true

	val count: Int
		get() = if (focusedItemId != null) 1 else decoration.checkedItemsCount

	init {
		registryOwner.lifecycle.addObserver(StateEventObserver())
	}

	fun snapshot(): Set<Long> = (focusedItemId ?: peekCheckedIds()).toSet()

	fun peekCheckedIds(): LongSet {
		return focusedItemId ?: decoration.checkedItemsIds
	}

	fun clear() {
		decoration.clearSelection()
		notifySelectionChanged()
	}

	fun addAll(ids: Collection<Long>) {
		if (ids.isEmpty()) {
			return
		}
		startActionMode()
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

	fun onItemLongClick(view: View, id: Long): Boolean {
		return if (useActionMode) {
			startSelection(id)
		} else {
			onItemContextClick(view, id)
		}
	}

	fun onItemContextClick(view: View, id: Long): Boolean {
		focusedItemId = longSetOf(id)
		val menu = PopupMenu(view.context, view)
		callback.onCreateActionMode(this, menu.menuInflater, menu.menu)
		callback.onPrepareActionMode(this, null, menu.menu)
		menu.setForceShowIcon(true)
		if (menu.menu.hasVisibleItems()) {
			menu.setOnMenuItemClickListener { menuItem ->
				callback.onActionItemClicked(this, null, menuItem)
			}
			menu.setOnDismissListener {
				focusedItemId = null
			}
			menu.show()
			return true
		} else {
			focusedItemId = null
			return false
		}
	}

	fun startSelection(id: Long): Boolean = startActionMode()?.also {
		decoration.setItemIsChecked(id, true)
		notifySelectionChanged()
	} != null

	override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
		return callback.onCreateActionMode(this, mode.menuInflater, menu)
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

	private fun startActionMode(): ActionMode? {
		focusedItemId = null
		return actionMode ?: appCompatDelegate.startSupportActionMode(this).also {
			actionMode = it
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

	interface Callback {

		fun onSelectionChanged(controller: ListSelectionController, count: Int)

		fun onCreateActionMode(controller: ListSelectionController, menuInflater: MenuInflater, menu: Menu): Boolean

		fun onPrepareActionMode(controller: ListSelectionController, mode: ActionMode?, menu: Menu): Boolean {
			mode?.title = controller.count.toString()
			return true
		}

		fun onActionItemClicked(controller: ListSelectionController, mode: ActionMode?, item: MenuItem): Boolean

		fun onDestroyActionMode(controller: ListSelectionController, mode: ActionMode) = Unit
	}

	private inner class StateEventObserver : LifecycleEventObserver {

		override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
			if (event == Lifecycle.Event.ON_CREATE) {
				source.lifecycle.removeObserver(this)
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
