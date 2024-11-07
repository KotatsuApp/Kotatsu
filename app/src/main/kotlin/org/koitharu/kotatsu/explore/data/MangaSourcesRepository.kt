package org.koitharu.kotatsu.explore.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.dao.MangaSourcesDao
import org.koitharu.kotatsu.core.db.entity.MangaSourceEntity
import org.koitharu.kotatsu.core.model.MangaSourceInfo
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.model.isNsfw
import org.koitharu.kotatsu.core.parser.external.ExternalMangaSource
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.ui.util.ReversibleHandle
import org.koitharu.kotatsu.core.util.ext.flattenLatest
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.mapNotNullToSet
import org.koitharu.kotatsu.parsers.util.mapToSet
import java.util.Collections
import java.util.EnumSet
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaSourcesRepository @Inject constructor(
	@ApplicationContext private val context: Context,
	private val db: MangaDatabase,
	private val settings: AppSettings,
) {

	private val isNewSourcesAssimilated = AtomicBoolean(false)
	private val dao: MangaSourcesDao
		get() = db.getSourcesDao()

	val allMangaSources: Set<MangaParserSource> = Collections.unmodifiableSet(
		EnumSet.allOf(MangaParserSource::class.java).apply {
			if (!BuildConfig.DEBUG) {
				remove(MangaParserSource.DUMMY)
			}
		},
	)

	suspend fun getEnabledSources(): List<MangaSource> {
		assimilateNewSources()
		val order = settings.sourcesSortOrder
		return dao.findAllEnabled(order).toSources(settings.isNsfwContentDisabled, order).let { enabled ->
			val external = getExternalSources()
			val list = ArrayList<MangaSourceInfo>(enabled.size + external.size)
			external.mapTo(list) { MangaSourceInfo(it, isEnabled = true, isPinned = true) }
			list.addAll(enabled)
			list
		}
	}

	suspend fun getPinnedSources(): Set<MangaSource> {
		assimilateNewSources()
		val skipNsfw = settings.isNsfwContentDisabled
		return dao.findAllPinned().mapNotNullToSet {
			it.source.toMangaSourceOrNull()?.takeUnless { x -> skipNsfw && x.isNsfw() }
		}
	}

	suspend fun getTopSources(limit: Int): List<MangaSource> {
		assimilateNewSources()
		return dao.findLastUsed(limit).toSources(settings.isNsfwContentDisabled, null)
	}

	suspend fun getDisabledSources(): Set<MangaSource> {
		assimilateNewSources()
		val result = EnumSet.copyOf(allMangaSources)
		val enabled = dao.findAllEnabledNames()
		for (name in enabled) {
			val source = name.toMangaSourceOrNull() ?: continue
			result.remove(source)
		}
		return result
	}

	suspend fun queryParserSources(
		isDisabledOnly: Boolean,
		isNewOnly: Boolean,
		excludeBroken: Boolean,
		types: Set<ContentType>,
		query: String?,
		locale: String?,
		sortOrder: SourcesSortOrder?,
	): List<MangaParserSource> {
		assimilateNewSources()
		val entities = dao.findAll().toMutableList()
		if (isDisabledOnly) {
			entities.removeAll { it.isEnabled }
		}
		if (isNewOnly) {
			entities.retainAll { it.addedIn == BuildConfig.VERSION_CODE }
		}
		val sources = entities.toSources(
			skipNsfwSources = settings.isNsfwContentDisabled,
			sortOrder = sortOrder,
		).run {
			mapNotNullTo(ArrayList(size)) { it.mangaSource as? MangaParserSource }
		}
		if (locale != null) {
			sources.retainAll { it.locale == locale }
		}
		if (excludeBroken) {
			sources.removeAll { it.isBroken }
		}
		if (types.isNotEmpty()) {
			sources.retainAll { it.contentType in types }
		}
		if (!query.isNullOrEmpty()) {
			sources.retainAll {
				it.getTitle(context).contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true)
			}
		}
		return sources
	}

	fun observeIsEnabled(source: MangaSource): Flow<Boolean> {
		return dao.observeIsEnabled(source.name).onStart { assimilateNewSources() }
	}

	fun observeEnabledSourcesCount(): Flow<Int> {
		return combine(
			observeIsNsfwDisabled(),
			dao.observeEnabled(SourcesSortOrder.MANUAL),
		) { skipNsfw, sources ->
			sources.count {
				it.source.toMangaSourceOrNull()?.let { s -> !skipNsfw || !s.isNsfw() } == true
			}
		}.distinctUntilChanged().onStart { assimilateNewSources() }
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
		}.distinctUntilChanged().onStart { assimilateNewSources() }
	}

	fun observeEnabledSources(): Flow<List<MangaSourceInfo>> = combine(
		observeIsNsfwDisabled(),
		observeSortOrder(),
	) { skipNsfw, order ->
		dao.observeEnabled(order).map {
			it.toSources(skipNsfw, order)
		}
	}.flattenLatest()
		.onStart { assimilateNewSources() }
		.combine(observeExternalSources()) { enabled, external ->
			val list = ArrayList<MangaSourceInfo>(enabled.size + external.size)
			external.mapTo(list) { MangaSourceInfo(it, isEnabled = true, isPinned = true) }
			list.addAll(enabled)
			list
		}

	fun observeAll(): Flow<List<Pair<MangaSource, Boolean>>> = dao.observeAll().map { entities ->
		val result = ArrayList<Pair<MangaSource, Boolean>>(entities.size)
		for (entity in entities) {
			val source = entity.source.toMangaSourceOrNull() ?: continue
			if (source in allMangaSources) {
				result.add(source to entity.isEnabled)
			}
		}
		result
	}.onStart { assimilateNewSources() }

	suspend fun setSourcesEnabled(sources: Collection<MangaSource>, isEnabled: Boolean): ReversibleHandle {
		setSourcesEnabledImpl(sources, isEnabled)
		return ReversibleHandle {
			setSourcesEnabledImpl(sources, !isEnabled)
		}
	}

	suspend fun setSourcesEnabledExclusive(sources: Set<MangaSource>) {
		db.withTransaction {
			assimilateNewSources()
			for (s in allMangaSources) {
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

	fun observeHasNewSources(): Flow<Boolean> = observeIsNsfwDisabled().map { skipNsfw ->
		val sources = dao.findAllFromVersion(BuildConfig.VERSION_CODE).toSources(skipNsfw, null)
		sources.isNotEmpty() && sources.size != allMangaSources.size
	}.onStart { assimilateNewSources() }

	fun observeHasNewSourcesForBadge(): Flow<Boolean> = combine(
		settings.observeAsFlow(AppSettings.KEY_SOURCES_VERSION) { sourcesVersion },
		observeIsNsfwDisabled(),
	) { version, skipNsfw ->
		if (version < BuildConfig.VERSION_CODE) {
			val sources = dao.findAllFromVersion(version).toSources(skipNsfw, null)
			sources.isNotEmpty()
		} else {
			false
		}
	}.onStart { assimilateNewSources() }

	fun clearNewSourcesBadge() {
		settings.sourcesVersion = BuildConfig.VERSION_CODE
	}

	private suspend fun assimilateNewSources(): Boolean {
		if (isNewSourcesAssimilated.getAndSet(true)) {
			return false
		}
		val new = getNewSources()
		if (new.isEmpty()) {
			return false
		}
		var maxSortKey = dao.getMaxSortKey()
		val entities = new.map { x ->
			MangaSourceEntity(
				source = x.name,
				isEnabled = false,
				sortKey = ++maxSortKey,
				addedIn = BuildConfig.VERSION_CODE,
				lastUsedAt = 0,
				isPinned = false,
			)
		}
		dao.insertIfAbsent(entities)
		return true
	}

	suspend fun isSetupRequired(): Boolean {
		return settings.sourcesVersion == 0 && dao.findAllEnabledNames().isEmpty()
	}

	suspend fun setIsPinned(sources: Collection<MangaSource>, isPinned: Boolean): ReversibleHandle {
		setSourcesPinnedImpl(sources, isPinned)
		return ReversibleHandle {
			setSourcesEnabledImpl(sources, !isPinned)
		}
	}

	suspend fun trackUsage(source: MangaSource) {
		if (!settings.isIncognitoModeEnabled && !(settings.isHistoryExcludeNsfw && source.isNsfw())) {
			dao.setLastUsed(source.name, System.currentTimeMillis())
		}
	}

	private suspend fun setSourcesEnabledImpl(sources: Collection<MangaSource>, isEnabled: Boolean) {
		if (sources.size == 1) { // fast path
			dao.setEnabled(sources.first().name, isEnabled)
			return
		}
		db.withTransaction {
			for (source in sources) {
				dao.setEnabled(source.name, isEnabled)
			}
		}
	}

	private suspend fun getNewSources(): MutableSet<out MangaSource> {
		val entities = dao.findAll()
		val result = EnumSet.copyOf(allMangaSources)
		for (e in entities) {
			result.remove(e.source.toMangaSourceOrNull() ?: continue)
		}
		return result
	}

	private suspend fun setSourcesPinnedImpl(sources: Collection<MangaSource>, isPinned: Boolean) {
		if (sources.size == 1) { // fast path
			dao.setPinned(sources.first().name, isPinned)
			return
		}
		db.withTransaction {
			for (source in sources) {
				dao.setPinned(source.name, isPinned)
			}
		}
	}

	private fun observeExternalSources(): Flow<List<ExternalMangaSource>> {
		return callbackFlow {
			val receiver = object : BroadcastReceiver() {
				override fun onReceive(context: Context?, intent: Intent?) {
					trySendBlocking(intent)
				}
			}
			ContextCompat.registerReceiver(
				context,
				receiver,
				IntentFilter().apply {
					addAction(Intent.ACTION_PACKAGE_ADDED)
					addAction(Intent.ACTION_PACKAGE_VERIFIED)
					addAction(Intent.ACTION_PACKAGE_REPLACED)
					addAction(Intent.ACTION_PACKAGE_REMOVED)
					addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
					addDataScheme("package")
				},
				ContextCompat.RECEIVER_EXPORTED,
			)
			awaitClose { context.unregisterReceiver(receiver) }
		}.onStart {
			emit(null)
		}.map {
			getExternalSources()
		}.distinctUntilChanged()
			.conflate()
	}

	private fun getExternalSources() = context.packageManager.queryIntentContentProviders(
		Intent("app.kotatsu.parser.PROVIDE_MANGA"), 0,
	).map { resolveInfo ->
		ExternalMangaSource(
			packageName = resolveInfo.providerInfo.packageName,
			authority = resolveInfo.providerInfo.authority,
		)
	}

	private fun List<MangaSourceEntity>.toSources(
		skipNsfwSources: Boolean,
		sortOrder: SourcesSortOrder?,
	): MutableList<MangaSourceInfo> {
		val result = ArrayList<MangaSourceInfo>(size)
		for (entity in this) {
			val source = entity.source.toMangaSourceOrNull() ?: continue
			if (skipNsfwSources && source.isNsfw()) {
				continue
			}
			if (source in allMangaSources) {
				result.add(
					MangaSourceInfo(
						mangaSource = source,
						isEnabled = entity.isEnabled,
						isPinned = entity.isPinned,
					),
				)
			}
		}
		if (sortOrder == SourcesSortOrder.ALPHABETIC) {
			result.sortWith(compareBy<MangaSourceInfo> { !it.isPinned }.thenBy { it.getTitle(context) })
		}
		return result
	}

	private fun observeIsNsfwDisabled() = settings.observeAsFlow(AppSettings.KEY_DISABLE_NSFW) {
		isNsfwContentDisabled
	}

	private fun observeSortOrder() = settings.observeAsFlow(AppSettings.KEY_SOURCES_ORDER) {
		sourcesSortOrder
	}

	private fun String.toMangaSourceOrNull(): MangaParserSource? = MangaParserSource.entries.find { it.name == this }
}
