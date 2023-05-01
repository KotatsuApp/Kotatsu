package org.koitharu.kotatsu.download.domain

import android.graphics.drawable.Drawable
import androidx.work.Data
import org.koitharu.kotatsu.parsers.model.Manga
import java.util.UUID

sealed interface DownloadState {

	val uuid: UUID
	val manga: Manga

	@Deprecated("")
	val cover: Drawable? get() = null

	@Deprecated("")
	val startId: Int get() = uuid.hashCode()

	fun toWorkData(): Data = Data.Builder()
		.putString(DATA_UUID, uuid.toString())
		.putLong(DATA_MANGA_ID, manga.id)
		.build()

	override fun equals(other: Any?): Boolean

	override fun hashCode(): Int

	val isTerminal: Boolean
		get() = this is Done || this is Cancelled || (this is Error && !canRetry)

	class Queued(
		override val uuid: UUID,
		override val manga: Manga,
	) : DownloadState {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Queued

			if (uuid != other.uuid) return false
			if (manga != other.manga) return false

			return true
		}

		override fun hashCode(): Int {
			var result = uuid.hashCode()
			result = 31 * result + manga.hashCode()
			return result
		}
	}

	class Preparing(
		override val uuid: UUID,
		override val manga: Manga,
	) : DownloadState {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Preparing

			if (uuid != other.uuid) return false
			if (manga != other.manga) return false

			return true
		}

		override fun hashCode(): Int {
			var result = uuid.hashCode()
			result = 31 * result + manga.hashCode()
			return result
		}
	}

	class Progress(
		override val uuid: UUID,
		override val manga: Manga,
		val totalChapters: Int,
		val currentChapter: Int,
		val totalPages: Int,
		val currentPage: Int,
		val timeLeft: Long,
	) : DownloadState {

		val max: Int = totalChapters * totalPages

		val progress: Int = totalPages * currentChapter + currentPage + 1

		val percent: Float = progress.toFloat() / max

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Progress

			if (uuid != other.uuid) return false
			if (manga != other.manga) return false
			if (totalChapters != other.totalChapters) return false
			if (currentChapter != other.currentChapter) return false
			if (totalPages != other.totalPages) return false
			if (currentPage != other.currentPage) return false

			return true
		}

		override fun hashCode(): Int {
			var result = uuid.hashCode()
			result = 31 * result + manga.hashCode()
			result = 31 * result + totalChapters
			result = 31 * result + currentChapter
			result = 31 * result + totalPages
			result = 31 * result + currentPage
			return result
		}
	}

	class Done(
		override val uuid: UUID,
		override val manga: Manga,
		val localManga: Manga,
	) : DownloadState {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Done

			if (uuid != other.uuid) return false
			if (manga != other.manga) return false
			if (localManga != other.localManga) return false

			return true
		}

		override fun hashCode(): Int {
			var result = uuid.hashCode()
			result = 31 * result + manga.hashCode()
			result = 31 * result + localManga.hashCode()
			return result
		}
	}

	class Error(
		override val uuid: UUID,
		override val manga: Manga,

		val error: Throwable,
		val canRetry: Boolean,
	) : DownloadState {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Error

			if (uuid != other.uuid) return false
			if (manga != other.manga) return false
			if (error != other.error) return false
			if (canRetry != other.canRetry) return false

			return true
		}

		override fun hashCode(): Int {
			var result = uuid.hashCode()
			result = 31 * result + manga.hashCode()
			result = 31 * result + error.hashCode()
			result = 31 * result + canRetry.hashCode()
			return result
		}
	}

	class Cancelled(
		override val uuid: UUID,
		override val manga: Manga,
	) : DownloadState {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Cancelled

			if (uuid != other.uuid) return false
			if (manga != other.manga) return false

			return true
		}

		override fun hashCode(): Int {
			var result = uuid.hashCode()
			result = 31 * result + manga.hashCode()
			return result
		}
	}

	class PostProcessing(
		override val uuid: UUID,
		override val manga: Manga,
	) : DownloadState {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as PostProcessing

			if (uuid != other.uuid) return false
			if (manga != other.manga) return false

			return true
		}

		override fun hashCode(): Int {
			var result = uuid.hashCode()
			result = 31 * result + manga.hashCode()
			return result
		}
	}

	companion object {

		private const val DATA_UUID = "id"
		private const val DATA_MANGA_ID = "manga_id"
	}
}
