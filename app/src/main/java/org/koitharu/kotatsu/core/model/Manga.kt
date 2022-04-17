package org.koitharu.kotatsu.core.model

import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.mapToSet

fun Collection<Manga>.ids() = mapToSet { it.id }