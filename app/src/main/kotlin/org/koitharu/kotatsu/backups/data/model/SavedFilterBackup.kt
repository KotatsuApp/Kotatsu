package org.koitharu.kotatsu.backups.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koitharu.kotatsu.core.model.MangaSourceSerializer
import org.koitharu.kotatsu.filter.data.MangaListFilterSerializer
import org.koitharu.kotatsu.filter.data.PersistableFilter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaSource

@Serializable
data class SavedFilterBackup(
    @SerialName("name")
    val name: String,
    @Serializable(with = MangaSourceSerializer::class)
    @SerialName("source")
    val source: MangaSource,
    @Serializable(with = MangaListFilterSerializer::class)
    @SerialName("filter")
    val filter: MangaListFilter,
) {

    constructor(persistableFilter: PersistableFilter) : this(
        name = persistableFilter.name,
        source = persistableFilter.source,
        filter = persistableFilter.filter,
    )

    fun toPersistableFilter() = PersistableFilter(
        name = name,
        source = source,
        filter = filter,
    )
}
