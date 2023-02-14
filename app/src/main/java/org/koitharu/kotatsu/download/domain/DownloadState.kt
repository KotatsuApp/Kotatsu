package org.koitharu.kotatsu.download.domain

import android.graphics.drawable.Drawable
import org.koitharu.kotatsu.parsers.model.Manga

sealed interface DownloadState {

	val startId: Int
	val manga: Manga
	val cover: Drawable?

	override fun equals(other: Any?): Boolean

	override fun hashCode(): Int

	val isTerminal: Boolean
		get() = this is Done || this is Cancelled || (this is Error && !canRetry)

	class Queued(
		override val startId: Int,
		override val manga: Manga,
		override val cover: Drawable?,
	) : DownloadState {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Queued

			if (startId != other.startId) return false
			if (manga != other.manga) return false
			if (cover != other.cover) return false

			return true
		}

		override fun hashCode(): Int {
			var result = startId
			result = 31 * result + manga.hashCode()
			result = 31 * result + (cover?.hashCode() ?: 0)
			return result
		}
	}

	class Preparing(
		override val startId: Int,
		override val manga: Manga,
		override val cover: Drawable?,
	) : DownloadState {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Preparing

			if (startId != other.startId) return false
			if (manga != other.manga) return false
			if (cover != other.cover) return false

			return true
		}

		override fun hashCode(): Int {
			var result = startId
			result = 31 * result + manga.hashCode()
			result = 31 * result + (cover?.hashCode() ?: 0)
			return result
		}
	}

	class Progress(
		override val startId: Int,
		override val manga: Manga,
		override val cover: Drawable?,
		val totalChapters: Int,
		val currentChapter: Int,
		val totalPages: Int,
		val currentPage: Int,
	) : DownloadState {

		val max: Int = totalChapters * totalPages

		val progress: Int = totalPages * currentChapter + currentPage + 1

		val percent: Float = progress.toFloat() / max

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Progress

			if (startId != other.startId) return false
			if (manga != other.manga) return false
			if (cover != other.cover) return false
			if (totalChapters != other.totalChapters) return false
			if (currentChapter != other.currentChapter) return false
			if (totalPages != other.totalPages) return false
			if (currentPage != other.currentPage) return false

			return true
		}

		override fun hashCode(): Int {
			var result = startId
			result = 31 * result + manga.hashCode()
			result = 31 * result + (cover?.hashCode() ?: 0)
			result = 31 * result + totalChapters
			result = 31 * result + currentChapter
			result = 31 * result + totalPages
			result = 31 * result + currentPage
			return result
		}
	}

	class Done(
		override val startId: Int,
		override val manga: Manga,
		override val cover: Drawable?,
		val localManga: Manga,
	) : DownloadState {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Done

			if (startId != other.startId) return false
			if (manga != other.manga) return false
			if (cover != other.cover) return false
			if (localManga != other.localManga) return false

			return true
		}

		override fun hashCode(): Int {
			var result = startId
			result = 31 * result + manga.hashCode()
			result = 31 * result + (cover?.hashCode() ?: 0)
			result = 31 * result + localManga.hashCode()
			return result
		}
	}

	class Error(
		override val startId: Int,
		override val manga: Manga,
		override val cover: Drawable?,
		val error: Throwable,
		val canRetry: Boolean,
	) : DownloadState {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Error

			if (startId != other.startId) return false
			if (manga != other.manga) return false
			if (cover != other.cover) return false
			if (error != other.error) return false
			if (canRetry != other.canRetry) return false

			return true
		}

		override fun hashCode(): Int {
			var result = startId
			result = 31 * result + manga.hashCode()
			result = 31 * result + (cover?.hashCode() ?: 0)
			result = 31 * result + error.hashCode()
			result = 31 * result + canRetry.hashCode()
			return result
		}
	}

	class Cancelled(
		override val startId: Int,
		override val manga: Manga,
		override val cover: Drawable?,
	) : DownloadState {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Cancelled

			if (startId != other.startId) return false
			if (manga != other.manga) return false
			if (cover != other.cover) return false

			return true
		}

		override fun hashCode(): Int {
			var result = startId
			result = 31 * result + manga.hashCode()
			result = 31 * result + (cover?.hashCode() ?: 0)
			return result
		}
	}

	class PostProcessing(
		override val startId: Int,
		override val manga: Manga,
		override val cover: Drawable?,
	) : DownloadState {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as PostProcessing

			if (startId != other.startId) return false
			if (manga != other.manga) return false
			if (cover != other.cover) return false

			return true
		}

		override fun hashCode(): Int {
			var result = startId
			result = 31 * result + manga.hashCode()
			result = 31 * result + (cover?.hashCode() ?: 0)
			return result
		}
	}
}
