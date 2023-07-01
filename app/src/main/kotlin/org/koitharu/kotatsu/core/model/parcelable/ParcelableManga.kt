package org.koitharu.kotatsu.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import org.koitharu.kotatsu.core.util.ext.readParcelableCompat
import org.koitharu.kotatsu.core.util.ext.readSerializableCompat
import org.koitharu.kotatsu.parsers.model.Manga

// Limits to avoid TransactionTooLargeException
private const val MAX_SAFE_SIZE = 1024 * 100 // Assume that 100 kb is safe parcel size
private const val MAX_SAFE_CHAPTERS_COUNT = 24 // this is 100% safe

@Parcelize
data class ParcelableManga(
	val manga: Manga,
	private val withChapters: Boolean,
) : Parcelable {
	companion object : Parceler<ParcelableManga> {
		private fun Manga.writeToParcel(out: Parcel, flags: Int, withChapters: Boolean) {
			out.writeLong(id)
			out.writeString(title)
			out.writeString(altTitle)
			out.writeString(url)
			out.writeString(publicUrl)
			out.writeFloat(rating)
			ParcelCompat.writeBoolean(out, isNsfw)
			out.writeString(coverUrl)
			out.writeString(largeCoverUrl)
			out.writeString(description)
			out.writeParcelable(ParcelableMangaTags(tags), flags)
			out.writeSerializable(state)
			out.writeString(author)
			val parcelableChapters = if (withChapters) null else chapters?.let(::ParcelableMangaChapters)
			out.writeParcelable(parcelableChapters, flags)
			out.writeSerializable(source)
		}

		override fun ParcelableManga.write(parcel: Parcel, flags: Int) {
			val chapters = manga.chapters
			if (!withChapters || chapters == null) {
				manga.writeToParcel(parcel, flags, withChapters = false)
				return
			}
			if (chapters.size <= MAX_SAFE_CHAPTERS_COUNT) {
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
				chapters = parcel.readParcelableCompat<ParcelableMangaChapters>()?.chapters,
				source = requireNotNull(parcel.readSerializableCompat()),
			),
			withChapters = true
		)
	}
}
