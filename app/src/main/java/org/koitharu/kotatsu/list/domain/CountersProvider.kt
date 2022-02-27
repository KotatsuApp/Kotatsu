package org.koitharu.kotatsu.list.domain

fun interface CountersProvider {

	suspend fun getCounter(mangaId: Long): Int
}