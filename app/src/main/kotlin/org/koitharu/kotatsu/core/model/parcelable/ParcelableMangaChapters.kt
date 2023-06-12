package org.koitharu.kotatsu.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import org.koitharu.kotatsu.parsers.model.MangaChapter

class ParcelableMangaChapters(
	val chapters: List<MangaChapter>,
) : Parcelable {

	constructor(parcel: Parcel) : this(
		List(parcel.readInt()) { parcel.readMangaChapter() }
	)

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeInt(chapters.size)
		for (chapter in chapters) {
			chapter.writeToParcel(parcel)
		}
	}

	override fun describeContents(): Int {
		return 0
	}

	companion object CREATOR : Parcelable.Creator<ParcelableMangaChapters> {
		override fun createFromParcel(parcel: Parcel): ParcelableMangaChapters {
			return ParcelableMangaChapters(parcel)
		}

		override fun newArray(size: Int): Array<ParcelableMangaChapters?> {
			return arrayOfNulls(size)
		}
	}
}