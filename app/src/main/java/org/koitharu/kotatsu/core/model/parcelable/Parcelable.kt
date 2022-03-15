package org.koitharu.kotatsu.core.model.parcelable

import android.os.Parcel
import androidx.core.os.ParcelCompat
import org.koitharu.kotatsu.parsers.model.*

fun Manga.writeToParcel(out: Parcel, flags: Int) {
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
	out.writeParcelable(chapters?.let(::ParcelableMangaChapters), flags)
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
	tags = requireNotNull(readParcelable<ParcelableMangaTags>(ParcelableMangaTags::class.java.classLoader)).tags,
	state = readSerializable() as MangaState?,
	author = readString(),
	chapters = readParcelable<ParcelableMangaChapters>(ParcelableMangaChapters::class.java.classLoader)?.chapters,
	source = readSerializable() as MangaSource,
)

fun MangaPage.writeToParcel(out: Parcel) {
	out.writeLong(id)
	out.writeString(url)
	out.writeString(referer)
	out.writeString(preview)
	out.writeSerializable(source)
}

fun Parcel.readMangaPage() = MangaPage(
	id = readLong(),
	url = requireNotNull(readString()),
	referer = requireNotNull(readString()),
	preview = readString(),
	source = readSerializable() as MangaSource,
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
	source = readSerializable() as MangaSource,
)

fun MangaTag.writeToParcel(out: Parcel) {
	out.writeString(title)
	out.writeString(key)
	out.writeSerializable(source)
}

fun Parcel.readMangaTag() = MangaTag(
	title = requireNotNull(readString()),
	key = requireNotNull(readString()),
	source = readSerializable() as MangaSource,
)