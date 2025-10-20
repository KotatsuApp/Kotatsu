package org.koitharu.kotatsu.filter.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.util.ext.toLocaleOrNull
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.Demographic
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import java.util.Locale

object MangaListFilterSerializer : KSerializer<MangaListFilter> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor(MangaListFilter::class.java.name) {
            element<String?>("query", isOptional = true)
            element(
                elementName = "tags",
                descriptor = SetSerializer(MangaTagSerializer).descriptor,
                isOptional = true,
            )
            element(
                elementName = "tagsExclude",
                descriptor = SetSerializer(MangaTagSerializer).descriptor,
                isOptional = true,
            )
            element<String?>("locale", isOptional = true)
            element<String?>("originalLocale", isOptional = true)
            element<Set<MangaState>>("states", isOptional = true)
            element<Set<ContentRating>>("contentRating", isOptional = true)
            element<Set<ContentType>>("types", isOptional = true)
            element<Set<Demographic>>("demographics", isOptional = true)
            element<Int>("year", isOptional = true)
            element<Int>("yearFrom", isOptional = true)
            element<Int>("yearTo", isOptional = true)
            element<String?>("author", isOptional = true)
        }

    override fun serialize(
        encoder: Encoder,
        value: MangaListFilter
    ) = encoder.encodeStructure(descriptor) {
        encodeNullableSerializableElement(descriptor, 0, String.serializer(), value.query)
        encodeSerializableElement(descriptor, 1, SetSerializer(MangaTagSerializer), value.tags)
        encodeSerializableElement(descriptor, 2, SetSerializer(MangaTagSerializer), value.tagsExclude)
        encodeNullableSerializableElement(descriptor, 3, String.serializer(), value.locale?.toLanguageTag())
        encodeNullableSerializableElement(descriptor, 4, String.serializer(), value.originalLocale?.toLanguageTag())
        encodeSerializableElement(descriptor, 5, SetSerializer(serializer()), value.states)
        encodeSerializableElement(descriptor, 6, SetSerializer(serializer()), value.contentRating)
        encodeSerializableElement(descriptor, 7, SetSerializer(serializer()), value.types)
        encodeSerializableElement(descriptor, 8, SetSerializer(serializer()), value.demographics)
        encodeIntElement(descriptor, 9, value.year)
        encodeIntElement(descriptor, 10, value.yearFrom)
        encodeIntElement(descriptor, 11, value.yearTo)
        encodeNullableSerializableElement(descriptor, 12, String.serializer(), value.author)
    }

    override fun deserialize(
        decoder: Decoder
    ): MangaListFilter = decoder.decodeStructure(descriptor) {
        var query: String? = MangaListFilter.EMPTY.query
        var tags: Set<MangaTag> = MangaListFilter.EMPTY.tags
        var tagsExclude: Set<MangaTag> = MangaListFilter.EMPTY.tagsExclude
        var locale: Locale? = MangaListFilter.EMPTY.locale
        var originalLocale: Locale? = MangaListFilter.EMPTY.originalLocale
        var states: Set<MangaState> = MangaListFilter.EMPTY.states
        var contentRating: Set<ContentRating> = MangaListFilter.EMPTY.contentRating
        var types: Set<ContentType> = MangaListFilter.EMPTY.types
        var demographics: Set<Demographic> = MangaListFilter.EMPTY.demographics
        var year: Int = MangaListFilter.EMPTY.year
        var yearFrom: Int = MangaListFilter.EMPTY.yearFrom
        var yearTo: Int = MangaListFilter.EMPTY.yearTo
        var author: String? = MangaListFilter.EMPTY.author

        while (true) {
            when (decodeElementIndex(descriptor)) {
                0 -> query = decodeNullableSerializableElement(descriptor, 0, serializer<String>())
                1 -> tags = decodeSerializableElement(descriptor, 1, SetSerializer(MangaTagSerializer))
                2 -> tagsExclude = decodeSerializableElement(descriptor, 2, SetSerializer(MangaTagSerializer))
                3 -> locale = decodeNullableSerializableElement(descriptor, 3, serializer<String>())?.toLocaleOrNull()
                4 -> originalLocale =
                    decodeNullableSerializableElement(descriptor, 4, serializer<String>())?.toLocaleOrNull()

                5 -> states = decodeSerializableElement(descriptor, 5, SetSerializer(serializer()))
                6 -> contentRating = decodeSerializableElement(descriptor, 6, SetSerializer(serializer()))
                7 -> types = decodeSerializableElement(descriptor, 7, SetSerializer(serializer()))
                8 -> demographics = decodeSerializableElement(descriptor, 8, SetSerializer(serializer()))
                9 -> year = decodeIntElement(descriptor, 9)
                10 -> yearFrom = decodeIntElement(descriptor, 10)
                11 -> yearTo = decodeIntElement(descriptor, 11)
                12 -> author = decodeNullableSerializableElement(descriptor, 12, serializer<String>())
                CompositeDecoder.DECODE_DONE -> break
            }
        }

        MangaListFilter(
            query = query,
            tags = tags,
            tagsExclude = tagsExclude,
            locale = locale,
            originalLocale = originalLocale,
            states = states,
            contentRating = contentRating,
            types = types,
            demographics = demographics,
            year = year,
            yearFrom = yearFrom,
            yearTo = yearTo,
            author = author,
        )
    }

    private object MangaTagSerializer : KSerializer<MangaTag> {

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(MangaTag::class.java.name) {
            element<String>("title")
            element<String>("key")
            element<String>("source")
        }

        override fun serialize(encoder: Encoder, value: MangaTag) = encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.title)
            encodeStringElement(descriptor, 1, value.key)
            encodeStringElement(descriptor, 2, value.source.name)
        }

        override fun deserialize(decoder: Decoder): MangaTag = decoder.decodeStructure(descriptor) {
            var title: String? = null
            var key: String? = null
            var source: String? = null

            while (true) {
                when (decodeElementIndex(descriptor)) {
                    0 -> title = decodeStringElement(descriptor, 0)
                    1 -> key = decodeStringElement(descriptor, 1)
                    2 -> source = decodeStringElement(descriptor, 2)
                    CompositeDecoder.DECODE_DONE -> break
                }
            }

            MangaTag(
                title = title ?: error("Missing 'title' field"),
                key = key ?: error("Missing 'key' field"),
                source = MangaSource(source),
            )
        }
    }
}
