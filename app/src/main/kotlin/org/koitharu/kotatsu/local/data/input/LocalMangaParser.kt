package org.koitharu.kotatsu.local.data.input

import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import okio.openZip
import org.jetbrains.annotations.Blocking
import org.koitharu.kotatsu.core.model.LocalMangaSource
import org.koitharu.kotatsu.core.util.AlphanumComparator
import org.koitharu.kotatsu.core.util.MimeTypes
import org.koitharu.kotatsu.core.util.ext.URI_SCHEME_ZIP
import org.koitharu.kotatsu.core.util.ext.isDirectory
import org.koitharu.kotatsu.core.util.ext.isFileUri
import org.koitharu.kotatsu.core.util.ext.isImage
import org.koitharu.kotatsu.core.util.ext.isRegularFile
import org.koitharu.kotatsu.core.util.ext.isZipUri
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.toFileNameSafe
import org.koitharu.kotatsu.core.util.ext.toListSorted
import org.koitharu.kotatsu.local.data.MangaIndex
import org.koitharu.kotatsu.local.data.hasZipExtension
import org.koitharu.kotatsu.local.data.isZipArchive
import org.koitharu.kotatsu.local.data.output.LocalMangaOutput.Companion.ENTRY_NAME_INDEX
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.util.longHashCode
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.parsers.util.toTitleCase
import java.io.File

/**
 * Manga root {dir or zip file}
 * |--- index.json (optional)
 * |--- Page 1.png
 * |--- Page 2.png
 * |---Chapter 1/(dir or zip, optional)
 * |------Page 1.1.png
 * :
 * L--- Page x.png
 */
class LocalMangaParser(private val uri: Uri) {

	constructor(file: File) : this(file.toUri())

	private val rootFile: File = File(uri.schemeSpecificPart)

	suspend fun getManga(withDetails: Boolean): LocalManga = runInterruptible(Dispatchers.IO) {
		(uri.resolveFsAndPath()).use { (fileSystem, rootPath) ->
			val index = MangaIndex.read(fileSystem, rootPath / ENTRY_NAME_INDEX)
			val mangaInfo = index?.getMangaInfo()
			if (mangaInfo != null) {
				val coverEntry: Path? = index.getCoverEntry()?.let { rootPath / it }?.takeIf {
					fileSystem.exists(it)
				}
				mangaInfo.copy(
					source = LocalMangaSource,
					url = rootFile.toUri().toString(),
					coverUrl = coverEntry?.let {
						uri.child(it, resolve = true).toString()
					} ?: fileSystem.findFirstImageUri(rootPath)?.toString(),
					largeCoverUrl = null,
					chapters = if (withDetails) {
						mangaInfo.chapters?.mapNotNull { c ->
							val path = index.getChapterFileName(c.id)?.toPath()
							if (path != null && !fileSystem.exists(rootPath / path)) {
								null
							} else {
								c.copy(
									url = path?.let {
										uri.child(it, resolve = false).toString()
									} ?: uri.toString(),
									source = LocalMangaSource,
								)
							}
						}
					} else {
						null
					},
				)
			} else {
				val title = rootFile.name.fileNameToTitle()
				Manga(
					id = rootFile.absolutePath.longHashCode(),
					title = title,
					url = rootFile.toUri().toString(),
					publicUrl = rootFile.toUri().toString(),
					source = LocalMangaSource,
					coverUrl = fileSystem.findFirstImageUri(rootPath)?.toString(),
					chapters = if (withDetails) {
						val chapters = fileSystem.listRecursively(rootPath)
							.mapNotNullTo(HashSet()) { path ->
								when {
									!fileSystem.isRegularFile(path) -> null
									path.isImage() -> path.parent
									hasZipExtension(path.name) -> path
									else -> null
								}
							}.sortedWith(compareBy(AlphanumComparator()) { x -> x.toString() })
						chapters.mapIndexed { i, p ->
							val s = if (p.root == rootPath.root) {
								p.relativeTo(rootPath).toString()
							} else {
								p
							}.toString().removePrefix(Path.DIRECTORY_SEPARATOR)
							MangaChapter(
								id = "$i$s".longHashCode(),
								title = p.userFriendlyName(),
								number = 0f,
								volume = 0,
								source = LocalMangaSource,
								uploadDate = 0L,
								url = uri.child(p.relativeTo(rootPath), resolve = false).toString(),
								scanlator = null,
								branch = null,
							)
						}
					} else {
						null
					},
					altTitles = emptySet(),
					rating = -1f,
					contentRating = null,
					tags = emptySet(),
					state = null,
					authors = emptySet(),
					largeCoverUrl = null,
					description = null,
				)
			}.let { LocalManga(it, rootFile) }
		}
	}

	suspend fun getMangaInfo(): Manga? = runInterruptible(Dispatchers.IO) {
		uri.resolveFsAndPath().use { (fileSystem, rootPath) ->
			val index = MangaIndex.read(fileSystem, rootPath / ENTRY_NAME_INDEX)
			index?.getMangaInfo()
		}
	}

	suspend fun getPages(chapter: MangaChapter): List<MangaPage> = runInterruptible(Dispatchers.IO) {
		val chapterUri = chapter.url.toUri().resolve()
		chapterUri.resolveFsAndPath().use { (fileSystem, rootPath) ->
			val index = MangaIndex.read(fileSystem, rootPath / ENTRY_NAME_INDEX)
			val entries = fileSystem.listRecursively(rootPath)
				.filter { fileSystem.isRegularFile(it) }
			if (index != null) {
				val pattern = index.getChapterNamesPattern(chapter)
				entries.filter { x -> x.name.substringBefore('.').matches(pattern) }
			} else {
				entries.filter { x -> x.isImage() && x.parent == rootPath }
			}.toListSorted(compareBy(AlphanumComparator()) { x -> x.toString() })
				.map { x ->
					val entryUri = chapterUri.child(x, resolve = true).toString()
					MangaPage(
						id = entryUri.longHashCode(),
						url = entryUri,
						preview = null,
						source = LocalMangaSource,
					)
				}
		}
	}

	private fun Uri.child(path: Path, resolve: Boolean): Uri {
		val file = fileFromPath()
		val builder = buildUpon()
		val isZip = isZipUri() || file.isZipArchive
		if (isZip) {
			builder.scheme(URI_SCHEME_ZIP)
		}
		if (isZip || !resolve) {
			builder.fragment(path.toString().removePrefix(Path.DIRECTORY_SEPARATOR))
		} else {
			builder.appendEncodedPath(path.relativeTo(file.toOkioPath()).toString())
		}
		return builder.build()
	}

	private fun FileSystem.findFirstImageUri(
		rootPath: Path,
		recursive: Boolean = false
	): Uri? = runCatchingCancellable {
		val list = list(rootPath)
		for (file in list.sortedWith(compareBy(AlphanumComparator()) { x -> x.name })) {
			if (isRegularFile(file)) {
				if (file.isImage()) {
					return@runCatchingCancellable uri.child(file, resolve = true)
				}
				if (recursive && file.isZip()) {
					openZip(file).use { zipFs ->
						zipFs.findFirstImageUri(Path.DIRECTORY_SEPARATOR.toPath())?.let { subUri ->
							val subPath = subUri.path.orEmpty().removePrefix(uri.path.orEmpty())
								.replace(REGEX_PARENT_PATH_PREFIX, "")
							return@runCatchingCancellable uri.child(file, resolve = true)
								.child(subPath.toPath(), resolve = false)
						}
					}
				}
			} else if (recursive && isDirectory(file)) {
				findFirstImageUri(file, true)?.let {
					return@runCatchingCancellable it
				}
			}
		}
		if (recursive) {
			null
		} else {
			findFirstImageUri(rootPath, recursive = true)
		}
	}.onFailure { e ->
		e.printStackTraceDebug()
	}.getOrNull()

	private fun Path.userFriendlyName(): String = name.substringBeforeLast('.')
		.replace('_', ' ')
		.toTitleCase()

	private class FsAndPath(
		val fileSystem: FileSystem,
		val path: Path,
		private val isCloseable: Boolean,
	) : AutoCloseable {

		override fun close() {
			if (isCloseable) {
				fileSystem.close()
			}
		}

		operator fun component1() = fileSystem

		operator fun component2() = path
	}

	companion object {

		private val REGEX_PARENT_PATH_PREFIX = Regex("^(/\\.\\.)+")

		@Blocking
		fun getOrNull(file: File): LocalMangaParser? = if ((file.isDirectory || file.isZipArchive) && file.canRead()) {
			LocalMangaParser(file)
		} else {
			null
		}

		suspend fun find(roots: Iterable<File>, manga: Manga): LocalMangaParser? = channelFlow {
			val fileName = manga.title.toFileNameSafe()
			for (root in roots) {
				launch {
					val parser = getOrNull(File(root, fileName)) ?: getOrNull(File(root, "$fileName.cbz"))
					val info = runCatchingCancellable { parser?.getMangaInfo() }.getOrNull()
					if (info?.id == manga.id) {
						send(parser)
					}
				}
			}
		}.flowOn(Dispatchers.Default).firstOrNull()

		private fun Path.isImage(): Boolean = MimeTypes.getMimeTypeFromExtension(name)?.isImage == true

		private fun Path.isZip(): Boolean = hasZipExtension(name)

		private fun Uri.resolve(): Uri = if (isFileUri()) {
			val file = toFile()
			if (file.isZipArchive) {
				this
			} else if (file.isDirectory) {
				file.resolve(fragment.orEmpty()).toUri()
			} else {
				this
			}
		} else {
			this
		}

		private fun Uri.fileFromPath(): File = File(requireNotNull(path) { "Uri path is null: $this" })

		@Blocking
		private fun Uri.resolveFsAndPath(): FsAndPath {
			val resolved = resolve()
			return when {
				resolved.isZipUri() -> FsAndPath(
					FileSystem.SYSTEM.openZip(resolved.schemeSpecificPart.toPath()),
					resolved.fragment.orEmpty().toRootedPath(),
					isCloseable = true,
				)

				isFileUri() -> {
					val file = toFile()
					if (file.isZipArchive) {
						FsAndPath(
							FileSystem.SYSTEM.openZip(schemeSpecificPart.toPath()),
							fragment.orEmpty().toRootedPath(),
							isCloseable = true,
						)
					} else {
						FsAndPath(FileSystem.SYSTEM, file.toOkioPath(), isCloseable = false)
					}
				}

				else -> throw IllegalArgumentException("Unsupported uri $resolved")
			}
		}

		private fun String.toRootedPath(): Path = if (startsWith(Path.DIRECTORY_SEPARATOR)) {
			this
		} else {
			Path.DIRECTORY_SEPARATOR + this
		}.toPath()

		private fun String.fileNameToTitle() = substringBeforeLast('.')
			.replace('_', ' ')
			.replaceFirstChar { it.uppercase() }
	}
}
