package org.koitharu.kotatsu.ui.reader

import android.net.Uri
import moxy.MvpView
import moxy.viewstate.strategy.alias.AddToEndSingle
import moxy.viewstate.strategy.alias.OneExecution
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.prefs.ReaderMode

interface ReaderView : MvpView {

	@AddToEndSingle
	fun onInitReader(mode: ReaderMode)

	@AddToEndSingle
	fun onChaptersLoader(chapters: List<MangaChapter>)

	@AddToEndSingle
	fun onPagesLoaded(chapterId: Long, pages: List<MangaPage>)

	@AddToEndSingle
	fun onLoadingStateChanged(isLoading: Boolean)

	@OneExecution
	fun onError(e: Exception)

	@OneExecution
	fun onPageSaved(uri: Uri?)
}