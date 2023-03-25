package org.koitharu.kotatsu.core.model.parcelable

import android.os.Parcel
import androidx.core.os.ParcelCompat
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.utils.ext.readParcelableCompat
import org.koitharu.kotatsu.utils.ext.readSerializableCompat

fun Manga.writeToParcel(out: Parcel, flags: Int, withChapters: Boolean) {
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
	if (withChapters) {
		out.writeParcelable(chapters?.let(::ParcelableMangaChapters), flags)
	} else {
		out.writeString(null)
	}
	out.writeSerializable(source)
}

fun Parcel.readManga() = Manga(
	id = readLong(),
	title = requireNotNull(readString()),
	altTitle = readString(),
	url = requireNotNull(readString()),
	publicUrl = requireNotNull(readString()),
	rating = readFloat(),
	isNsfw = ParcelCompat.readBoolean(this),
	coverUrl = requireNotNull(readString()),
	largeCoverUrl = readString(),
	description = readString(),
	tags = requireNotNull(readParcelableCompat<ParcelableMangaTags>()).tags,
	state = readSerializableCompat(),
	author = readString(),
	chapters = readParcelableCompat<ParcelableMangaChapters>()?.chapters,
	source = checkNotNull(readSerializableCompat()),
)

fun MangaPage.writeToParcel(out: Parcel) {
	out.writeLong(id)
	out.writeString(url)
	out.writeString(preview)
	out.writeSerializable(source)
}

fun Parcel.readMangaPage() = MangaPage(
	id = readLong(),
	url = requireNotNull(readString()),
	preview = readString(),
	source = checkNotNull(readSerializableCompat()),
)

fun MangaChapter.writeToParcel(out: Parcel) {
	out.writeLong(id)
	out.writeString(name)
	out.writeInt(number)
	out.writeString(url)
	out.writeString(scanlator)
	out.writeLong(uploadDate)
	out.writeString(branch)
	out.writeSerializable(source)
}

fun Parcel.readMangaChapter() = MangaChapter(
	id = readLong(),
	name = requireNotNull(readString()),
	number = readInt(),
	url = requireNotNull(readString()),
	scanlator = readString(),
	uploadDate = readLong(),
	branch = readString(),
	source = checkNotNull(readSerializableCompat()),
)

fun MangaTag.writeToParcel(out: Parcel) {
	out.writeString(title)
	out.writeString(key)
	out.writeSerializable(source)
}

fun Parcel.readMangaTag() = MangaTag(
	title = requireNotNull(readString()),
	key = requireNotNull(readString()),
	source = checkNotNull(readSerializableCompat()),
)
