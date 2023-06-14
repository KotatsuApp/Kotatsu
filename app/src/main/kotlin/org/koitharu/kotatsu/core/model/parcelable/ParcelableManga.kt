package org.koitharu.kotatsu.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import org.koitharu.kotatsu.parsers.model.Manga

// Limits to avoid TransactionTooLargeException
private const val MAX_SAFE_SIZE = 1024 * 100 // Assume that 100 kb is safe parcel size
private const val MAX_SAFE_CHAPTERS_COUNT = 24 // this is 100% safe

class ParcelableManga(
	val manga: Manga,
	private val withChapters: Boolean,
) : Parcelable {

	constructor(parcel: Parcel) : this(parcel.readManga(), true)

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		val chapters = manga.chapters
		if (!withChapters || chapters == null) {
			manga.writeToParcel(parcel, flags, withChapters = false)
			return
		}
		if (chapters.size <= MAX_SAFE_CHAPTERS_COUNT) {
			// fast path
			manga.writeToParcel(parcel, flags, withChapters = true)
			return
		}
		val tempParcel = Parcel.obtain()
		manga.writeToParcel(tempParcel, flags, withChapters = true)
		val size = tempParcel.dataSize()
		if (size < MAX_SAFE_SIZE) {
			parcel.appendFrom(tempParcel, 0, size)
		} else {
			manga.writeToParcel(parcel, flags, withChapters = false)
		}
		tempParcel.recycle()
	}

	override fun describeContents(): Int {
		return 0
	}

	override fun toString(): String {
		return "ParcelableManga(manga=$manga, withChapters=$withChapters)"
	}

	companion object CREATOR : Parcelable.Creator<ParcelableManga> {
		override fun createFromParcel(parcel: Parcel): ParcelableManga {
			return ParcelableManga(parcel)
		}

		override fun newArray(size: Int): Array<ParcelableManga?> {
			return arrayOfNulls(size)
		}
	}
}
