package org.koitharu.kotatsu.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import org.koitharu.kotatsu.parsers.model.Manga

class ParcelableManga(
	val manga: Manga,
): Parcelable {
	constructor(parcel: Parcel) : this(parcel.readManga())

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		manga.writeToParcel(parcel, flags)
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