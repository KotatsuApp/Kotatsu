package org.koitharu.kotatsu.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.utils.ext.Set

class ParcelableMangaTags(
	val tags: Set<MangaTag>,
) : Parcelable {

	constructor(parcel: Parcel) : this(
		Set(parcel.readInt()) { parcel.readMangaTag() }
	)

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeInt(tags.size)
		for (tag in tags) {
			tag.writeToParcel(parcel)
		}
	}

	override fun describeContents(): Int {
		return 0
	}

	companion object CREATOR : Parcelable.Creator<ParcelableMangaTags> {
		override fun createFromParcel(parcel: Parcel): ParcelableMangaTags {
			return ParcelableMangaTags(parcel)
		}

		override fun newArray(size: Int): Array<ParcelableMangaTags?> {
			return arrayOfNulls(size)
		}
	}
}