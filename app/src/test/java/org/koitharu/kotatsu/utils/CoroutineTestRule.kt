package org.koitharu.kotatsu.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class CoroutineTestRule(
	private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

	override fun starting(description: Description) {
		super.starting(description)
		Dispatchers.setMain(testDispatcher)
	}

	override fun finished(description: Description) {
		super.finished(description)
		Dispatchers.resetMain()
	}

	fun runBlockingTest(block: suspend CoroutineScope.() -> Unit) {
		runBlocking(testDispatcher) {
			block()
		}
	}
}