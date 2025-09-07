package org.koitharu.kotatsu.core.parser

import org.koitharu.kotatsu.core.cache.MemoryContentCache
import org.koitharu.kotatsu.core.model.TestMangaSource
import org.koitharu.kotatsu.parsers.MangaLoaderContext

@Suppress("unused")
class TestMangaRepository(
	private val loaderContext: MangaLoaderContext,
	cache: MemoryContentCache
) : EmptyMangaRepository(TestMangaSource)
