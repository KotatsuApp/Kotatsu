package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.koitharu.kotatsu.domain.MangaRepository
import org.koitharu.kotatsu.domain.repository.ReadmangaRepository

@Parcelize
enum class MangaSource(val title: String, val cls: Class<out MangaRepository>): Parcelable {
	READMANGA_RU("ReadManga", ReadmangaRepository::class.java)
}