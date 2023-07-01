package org.koitharu.kotatsu.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.koitharu.kotatsu.core.util.ext.readSerializableCompat
import org.koitharu.kotatsu.parsers.model.MangaPage

object MangaPageParceler : Parceler<MangaPage> {
	override fun create(parcel: Parcel) = MangaPage(
		id = parcel.readLong(),
		url = requireNotNull(parcel.readString()),
		preview = parcel.readString(),
		source = requireNotNull(parcel.readSerializableCompat()),
	)

	override fun MangaPage.write(parcel: Parcel, flags: Int) {
		parcel.writeLong(id)
		parcel.writeString(url)
		parcel.writeString(preview)
		parcel.writeSerializable(source)
	}
}

@Parcelize
@TypeParceler<MangaPage, MangaPageParceler>
class ParcelableMangaPage(val page: MangaPage) : Parcelable
