package org.koitharu.kotatsu.list.domain

interface ListExtraProvider {

	suspend fun getCounter(mangaId: Long): Int

	suspend fun getProgress(mangaId: Long): Float
}