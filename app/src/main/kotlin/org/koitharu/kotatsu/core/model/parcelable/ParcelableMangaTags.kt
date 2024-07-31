package org.koitharu.kotatsu.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag

object MangaTagParceler : Parceler<MangaTag> {
	override fun create(parcel: Parcel) = MangaTag(
		title = requireNotNull(parcel.readString()),
		key = requireNotNull(parcel.readString()),
		source = MangaSource(parcel.readString()),
	)

	override fun MangaTag.write(parcel: Parcel, flags: Int) {
		parcel.writeString(title)
		parcel.writeString(key)
		parcel.writeString(source.name)
	}
}

@Parcelize
@TypeParceler<MangaTag, MangaTagParceler>
data class ParcelableMangaTags(val tags: Set<MangaTag>) : Parcelable
