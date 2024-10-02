package org.koitharu.kotatsu.core.util

interface CloseableSequence<T> : Sequence<T>, AutoCloseable
