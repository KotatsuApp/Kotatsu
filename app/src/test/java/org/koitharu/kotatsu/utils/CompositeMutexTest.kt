package org.koitharu.kotatsu.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.Assert.assertNull
import org.junit.Test

class CompositeMutexTest {

	@Test
	fun testSingleLock() = runTest {
		val mutex = CompositeMutex<Int>()
		mutex.lock(1)
		mutex.lock(2)
		mutex.unlock(1)
		assert(mutex.size == 1)
		mutex.unlock(2)
		assert(mutex.isEmpty())
	}

	@Test
	fun testDoubleLock() = runTest {
		val mutex = CompositeMutex<Int>()
		repeat(2) {
			launch(Dispatchers.Default) {
				mutex.lock(1)
			}
		}
		yield()
		mutex.unlock(1)
		val tryLock = withTimeoutOrNull(1000) {
			mutex.lock(1)
		}
		assertNull(tryLock)
	}
}