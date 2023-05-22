package org.koitharu.kotatsu.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import org.koitharu.kotatsu.parsers.model.MangaPage

class ParcelableMangaPages(
	val pages: List<MangaPage>,
) : Parcelable {

	constructor(parcel: Parcel) : this(
		List(parcel.readInt()) { parcel.readMangaPage() }
	)

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeInt(pages.size)
		for (page in pages) {
			page.writeToParcel(parcel)
		}
	}

	override fun describeContents(): Int {
		return 0
	}

	companion object CREATOR : Parcelable.Creator<ParcelableMangaPages> {
		override fun createFromParcel(parcel: Parcel): ParcelableMangaPages {
			return ParcelableMangaPages(parcel)
		}

		override fun newArray(size: Int): Array<ParcelableMangaPages?> {
			return arrayOfNulls(size)
		}
	}
}