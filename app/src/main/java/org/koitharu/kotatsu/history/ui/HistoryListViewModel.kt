package org.koitharu.kotatsu.history.ui

import android.content.Context
import android.os.Build
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.utils.MangaShortcut
import org.koitharu.kotatsu.utils.SingleLiveEvent

class HistoryListViewModel(
	private val repository: HistoryRepository,
	private val context: Context //todo create ShortcutRepository
) : MangaListViewModel() {

	val onItemRemoved = SingleLiveEvent<Manga>()

	fun loadList(offset: Int) {
		launchLoadingJob {
			val list = repository.getList(offset = offset)
			if (offset == 0) {
				content.value = list
			} else {
				content.value = content.value.orEmpty() + list
			}
		}
	}

	fun clearHistory() {
		launchLoadingJob {
			repository.clear()
			content.value = emptyList()
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
				MangaShortcut.clearAppShortcuts(context)
			}
		}
	}

	fun removeFromHistory(manga: Manga) {
		launchJob {
			repository.delete(manga)
			content.value = content.value?.filterNot { it.id == manga.id }
			onItemRemoved.call(manga)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
				MangaShortcut(manga).removeAppShortcut(context)
			}
		}
	}

}