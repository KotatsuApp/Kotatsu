package org.koitharu.kotatsu.ui.common

import kotlinx.coroutines.*
import moxy.MvpPresenter
import moxy.MvpView
import org.koin.core.KoinComponent
import kotlin.coroutines.CoroutineContext

abstract class BasePresenter<V : MvpView> : MvpPresenter<V>(), KoinComponent, CoroutineScope {

	private val job = SupervisorJob()

	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Main + job

	override fun onDestroy() {
		coroutineContext.cancel()
		super.onDestroy()
	}
}