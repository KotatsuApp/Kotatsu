package org.koitharu.kotatsu.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.koitharu.kotatsu.parsers.model.MangaSource

object MangaSourceSerializer : KSerializer<MangaSource> {

    override val descriptor: SerialDescriptor = serialDescriptor<String>()

    override fun serialize(
        encoder: Encoder,
        value: MangaSource
    ) = encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder): MangaSource = MangaSource(decoder.decodeString())
}
