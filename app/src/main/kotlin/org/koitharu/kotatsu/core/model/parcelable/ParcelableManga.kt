package org.koitharu.kotatsu.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.util.ext.readParcelableCompat
import org.koitharu.kotatsu.core.util.ext.readSerializableCompat
import org.koitharu.kotatsu.parsers.model.Manga

@Parcelize
data class ParcelableManga(
	val manga: Manga,
) : Parcelable {

	companion object : Parceler<ParcelableManga> {

		override fun ParcelableManga.write(parcel: Parcel, flags: Int) = with(manga) {
			parcel.writeLong(id)
			parcel.writeString(title)
			parcel.writeString(altTitle)
			parcel.writeString(url)
			parcel.writeString(publicUrl)
			parcel.writeFloat(rating)
			ParcelCompat.writeBoolean(parcel, isNsfw)
			parcel.writeString(coverUrl)
			parcel.writeString(largeCoverUrl)
			parcel.writeString(description)
			parcel.writeParcelable(ParcelableMangaTags(tags), flags)
			parcel.writeSerializable(state)
			parcel.writeString(author)
			parcel.writeString(source.name)
		}

		override fun create(parcel: Parcel) = ParcelableManga(
			Manga(
				id = parcel.readLong(),
				title = requireNotNull(parcel.readString()),
				altTitle = parcel.readString(),
				url = requireNotNull(parcel.readString()),
				publicUrl = requireNotNull(parcel.readString()),
				rating = parcel.readFloat(),
				isNsfw = ParcelCompat.readBoolean(parcel),
				coverUrl = requireNotNull(parcel.readString()),
				largeCoverUrl = parcel.readString(),
				description = parcel.readString(),
				tags = requireNotNull(parcel.readParcelableCompat<ParcelableMangaTags>()).tags,
				state = parcel.readSerializableCompat(),
				author = parcel.readString(),
				chapters = null,
				source = MangaSource(parcel.readString()),
			),
		)
	}
}
