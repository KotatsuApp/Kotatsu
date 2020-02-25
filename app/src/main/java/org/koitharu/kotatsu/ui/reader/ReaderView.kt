package org.koitharu.kotatsu.ui.reader

import android.net.Uri
import moxy.MvpView
import moxy.viewstate.strategy.alias.AddToEndSingle
import moxy.viewstate.strategy.alias.OneExecution
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.prefs.ReaderMode

interface ReaderView : MvpView {

	@AddToEndSingle
	fun onInitReader(pages: List<MangaPage>, mode: ReaderMode, state: ReaderState)

	@AddToEndSingle
	fun onLoadingStateChanged(isLoading: Boolean)

	@OneExecution
	fun onError(e: Exception)

	@OneExecution
	fun onPageSaved(uri: Uri?)
}