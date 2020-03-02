package org.koitharu.kotatsu.ui.reader

import android.net.Uri
import moxy.viewstate.strategy.alias.AddToEndSingle
import moxy.viewstate.strategy.alias.OneExecution
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.ui.common.BaseMvpView

interface ReaderView : BaseMvpView {

	@AddToEndSingle
	fun onInitReader(mode: ReaderMode)

	@AddToEndSingle
	fun onChaptersLoader(chapters: List<MangaChapter>)

	@AddToEndSingle
	fun onPagesLoaded(chapterId: Long, pages: List<MangaPage>, action: ReaderAction)

	@OneExecution
	fun onPageSaved(uri: Uri?)
}