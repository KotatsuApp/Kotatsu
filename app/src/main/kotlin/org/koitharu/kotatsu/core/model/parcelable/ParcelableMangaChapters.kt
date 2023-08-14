package org.koitharu.kotatsu.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.koitharu.kotatsu.core.util.ext.readSerializableCompat
import org.koitharu.kotatsu.parsers.model.MangaChapter

object MangaChapterParceler : Parceler<MangaChapter> {
	override fun create(parcel: Parcel) = MangaChapter(
		id = parcel.readLong(),
		name = requireNotNull(parcel.readString()),
		number = parcel.readInt(),
		url = requireNotNull(parcel.readString()),
		scanlator = parcel.readString(),
		uploadDate = parcel.readLong(),
		branch = parcel.readString(),
		source = requireNotNull(parcel.readSerializableCompat()),
	)

	override fun MangaChapter.write(parcel: Parcel, flags: Int) {
		parcel.writeLong(id)
		parcel.writeString(name)
		parcel.writeInt(number)
		parcel.writeString(url)
		parcel.writeString(scanlator)
		parcel.writeLong(uploadDate)
		parcel.writeString(branch)
		parcel.writeSerializable(source)
	}
}

@Parcelize
@TypeParceler<MangaChapter, MangaChapterParceler>
data class ParcelableMangaChapters(val chapters: List<MangaChapter>) : Parcelable
