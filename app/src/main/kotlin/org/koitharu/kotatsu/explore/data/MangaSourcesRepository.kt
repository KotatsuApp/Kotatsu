package org.koitharu.kotatsu.explore.data

import androidx.room.withTransaction
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.dao.MangaSourcesDao
import org.koitharu.kotatsu.core.db.entity.MangaSourceEntity
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.isNsfw
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.ui.util.ReversibleHandle
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.mapToSet
import java.util.Collections
import java.util.EnumSet
import javax.inject.Inject

@Reusable
class MangaSourcesRepository @Inject constructor(
	private val db: MangaDatabase,
	private val settings: AppSettings,
) {

	private val dao: MangaSourcesDao
		get() = db.getSourcesDao()

	private val remoteSources = EnumSet.allOf(MangaSource::class.java).apply {
		remove(MangaSource.LOCAL)
		if (!BuildConfig.DEBUG) {
			remove(MangaSource.DUMMY)
		}
	}

	val allMangaSources: Set<MangaSource>
		get() = Collections.unmodifiableSet(remoteSources)

	suspend fun getEnabledSources(): List<MangaSource> {
		val order = settings.sourcesSortOrder
		return dao.findAllEnabled(order).toSources(settings.isNsfwContentDisabled, order)
	}

	suspend fun getDisabledSources(): List<MangaSource> {
		return dao.findAllDisabled().toSources(settings.isNsfwContentDisabled, null)
	}

	fun observeIsEnabled(source: MangaSource): Flow<Boolean> {
		return dao.observeIsEnabled(source.name)
	}

	fun observeEnabledSourcesCount(): Flow<Int> {
		return combine(
			observeIsNsfwDisabled(),
			dao.observeEnabled(SourcesSortOrder.MANUAL),
		) { skipNsfw, sources ->
			sources.count { !skipNsfw || !MangaSource(it.source).isNsfw() }
		}.distinctUntilChanged()
	}

	fun observeAvailableSourcesCount(): Flow<Int> {
		return combine(
			observeIsNsfwDisabled(),
			dao.observeEnabled(SourcesSortOrder.MANUAL),
		) { skipNsfw, enabledSources ->
			val enabled = enabledSources.mapToSet { it.source }
			allMangaSources.count { x ->
				x.name !in enabled && (!skipNsfw || !x.isNsfw())
			}
		}.distinctUntilChanged()
	}

	fun observeEnabledSources(): Flow<List<MangaSource>> = combine(
		observeIsNsfwDisabled(),
		observeSortOrder(),
	) { skipNsfw, order ->
		dao.observeEnabled(order).map {
			it.toSources(skipNsfw, order)
		}
	}.flatMapLatest { it }

	fun observeAll(): Flow<List<Pair<MangaSource, Boolean>>> = dao.observeAll().map { entities ->
		val result = ArrayList<Pair<MangaSource, Boolean>>(entities.size)
		for (entity in entities) {
			val source = MangaSource(entity.source)
			if (source in remoteSources) {
				result.add(source to entity.isEnabled)
			}
		}
		result
	}

	suspend fun setSourceEnabled(source: MangaSource, isEnabled: Boolean): ReversibleHandle {
		dao.setEnabled(source.name, isEnabled)
		return ReversibleHandle {
			dao.setEnabled(source.name, !isEnabled)
		}
	}

	suspend fun setSourcesEnabledExclusive(sources: Set<MangaSource>) {
		db.withTransaction {
			for (s in remoteSources) {
				dao.setEnabled(s.name, s in sources)
			}
		}
	}

	suspend fun disableAllSources() {
		db.withTransaction {
			assimilateNewSources()
			dao.disableAllSources()
		}
	}

	suspend fun setPositions(sources: List<MangaSource>) {
		db.withTransaction {
			for ((index, item) in sources.withIndex()) {
				dao.setSortKey(item.name, index)
			}
		}
	}

	fun observeNewSources(): Flow<Set<MangaSource>> = observeIsNewSourcesEnabled().flatMapLatest {
		if (it) {
			combine(
				dao.observeAll(),
				observeIsNsfwDisabled(),
			) { entities, skipNsfw ->
				val result = EnumSet.copyOf(remoteSources)
				for (e in entities) {
					result.remove(MangaSource(e.source))
				}
				if (skipNsfw) {
					result.removeAll { x -> x.isNsfw() }
				}
				result
			}.distinctUntilChanged()
		} else {
			flowOf(emptySet())
		}
	}

	suspend fun assimilateNewSources(): Set<MangaSource> {
		val new = getNewSources()
		if (new.isEmpty()) {
			return emptySet()
		}
		var maxSortKey = dao.getMaxSortKey()
		val entities = new.map { x ->
			MangaSourceEntity(
				source = x.name,
				isEnabled = false,
				sortKey = ++maxSortKey,
			)
		}
		dao.insertIfAbsent(entities)
		if (settings.isNsfwContentDisabled) {
			new.removeAll { x -> x.isNsfw() }
		}
		return new
	}

	suspend fun isSetupRequired(): Boolean {
		return dao.findAll().isEmpty()
	}

	private suspend fun getNewSources(): MutableSet<MangaSource> {
		val entities = dao.findAll()
		val result = EnumSet.copyOf(remoteSources)
		for (e in entities) {
			result.remove(MangaSource(e.source))
		}
		return result
	}

	private fun List<MangaSourceEntity>.toSources(
		skipNsfwSources: Boolean,
		sortOrder: SourcesSortOrder?,
	): List<MangaSource> {
		val result = ArrayList<MangaSource>(size)
		for (entity in this) {
			val source = MangaSource(entity.source)
			if (skipNsfwSources && source.contentType == ContentType.HENTAI) {
				continue
			}
			if (source in remoteSources) {
				result.add(source)
			}
		}
		if (sortOrder == SourcesSortOrder.ALPHABETIC) {
			result.sortBy { it.title }
		}
		return result
	}

	private fun observeIsNsfwDisabled() = settings.observeAsFlow(AppSettings.KEY_DISABLE_NSFW) {
		isNsfwContentDisabled
	}

	private fun observeIsNewSourcesEnabled() = settings.observeAsFlow(AppSettings.KEY_SOURCES_NEW) {
		isNewSourcesTipEnabled
	}

	private fun observeSortOrder() = settings.observeAsFlow(AppSettings.KEY_SOURCES_ORDER) {
		sourcesSortOrder
	}
}
