package org.koitharu.kotatsu.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.parsers.model.Manga

class ParcelableManga(
	val manga: Manga,
) : Parcelable {

	constructor(parcel: Parcel) : this(parcel.readManga())

	init {
		if (BuildConfig.DEBUG && manga.chapters != null) {
			Log.w("ParcelableManga", "Passing manga with chapters as Parcelable is dangerous!")
		}
	}

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