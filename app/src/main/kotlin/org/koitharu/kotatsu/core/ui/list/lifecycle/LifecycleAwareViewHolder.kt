package org.koitharu.kotatsu.core.ui.list.lifecycle

import android.view.View
import androidx.annotation.CallSuper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView

abstract class LifecycleAwareViewHolder(
	itemView: View,
	private val parentLifecycleOwner: LifecycleOwner,
) : RecyclerView.ViewHolder(itemView), LifecycleOwner {

	@Suppress("LeakingThis")
	final override val lifecycle = LifecycleRegistry(this)
	private var isCurrent = false

	init {
		itemView.post {
			parentLifecycleOwner.lifecycle.addObserver(ParentLifecycleObserver())
		}
	}

	fun setIsCurrent(value: Boolean) {
		isCurrent = value
		dispatchResumed()
	}

	@CallSuper
	open fun onCreate() = lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

	@CallSuper
	open fun onStart() = lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)

	@CallSuper
	open fun onResume() = lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

	@CallSuper
	open fun onPause() = lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)

	@CallSuper
	open fun onStop() = lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

	@CallSuper
	open fun onDestroy() = lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

	private fun dispatchResumed() {
		val isParentResumed = parentLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
		if (isCurrent && isParentResumed) {
			if (!isResumed()) {
				onResume()
			}
		} else {
			if (isResumed()) {
				onPause()
			}
		}
	}

	protected fun isResumed(): Boolean {
		return lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
	}

	private inner class ParentLifecycleObserver : DefaultLifecycleObserver {

		override fun onCreate(owner: LifecycleOwner) = this@LifecycleAwareViewHolder.onCreate()

		override fun onStart(owner: LifecycleOwner) = this@LifecycleAwareViewHolder.onStart()

		override fun onResume(owner: LifecycleOwner) = this@LifecycleAwareViewHolder.dispatchResumed()

		override fun onPause(owner: LifecycleOwner) = this@LifecycleAwareViewHolder.dispatchResumed()

		override fun onStop(owner: LifecycleOwner) = this@LifecycleAwareViewHolder.onStop()

		override fun onDestroy(owner: LifecycleOwner) {
			this@LifecycleAwareViewHolder.onDestroy()
			owner.lifecycle.removeObserver(this)
		}
	}
}
