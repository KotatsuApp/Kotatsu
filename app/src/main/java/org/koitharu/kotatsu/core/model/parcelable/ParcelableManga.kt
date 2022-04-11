package org.koitharu.kotatsu.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import org.koitharu.kotatsu.parsers.model.Manga

// Limits to avoid TransactionTooLargeException
private const val MAX_SAFE_SIZE = 1024 * 512 // Assume that 512 kb is safe parcel size
private const val MAX_SAFE_CHAPTERS_COUNT = 40 // this is 100% safe

class ParcelableManga(
	val manga: Manga,
) : Parcelable {

	constructor(parcel: Parcel) : this(parcel.readManga())

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		val chapters = manga.chapters
		if (chapters == null || chapters.size <= MAX_SAFE_CHAPTERS_COUNT) {
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

	companion object CREATOR : Parcelable.Creator<ParcelableManga> {
		override fun createFromParcel(parcel: Parcel): ParcelableManga {
			return ParcelableManga(parcel)
		}

		override fun newArray(size: Int): Array<ParcelableManga?> {
			return arrayOfNulls(size)
		}
	}
}