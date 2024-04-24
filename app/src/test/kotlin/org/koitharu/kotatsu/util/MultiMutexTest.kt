package org.koitharu.kotatsu.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Ignore
import org.junit.Test
import org.koitharu.kotatsu.core.util.MultiMutex

class MultiMutexTest {

	@Test
	fun singleLock() = runTest {
		val mutex = MultiMutex<Int>()
		mutex.lock(1)
		mutex.lock(2)
		mutex.unlock(1)
		assert(mutex.size == 1)
		mutex.unlock(2)
		assert(mutex.isEmpty())
	}

	@Test
	@Ignore("Cannot delay in test")
	fun doubleLock() = runTest {
		val mutex = MultiMutex<Int>()
		repeat(2) {
			launch(Dispatchers.Default) {
				mutex.lock(1)
			}
		}
		yield()
		assertEquals(1, mutex.size)
		mutex.unlock(1)
		val tryLock = withTimeoutOrNull(1000) {
			mutex.lock(1)
		}
		assertNull(tryLock)
	}

	@Test
	fun cancellation() = runTest {
		val mutex = MultiMutex<Int>()
		mutex.lock(1)
		val job = launch {
			try {
				mutex.lock(1)
			} finally {
				mutex.unlock(1)
			}
		}
		withTimeout(2000) {
			job.cancelAndJoin()
		}
	}
}
