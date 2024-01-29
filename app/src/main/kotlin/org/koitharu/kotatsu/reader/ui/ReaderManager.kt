package org.koitharu.kotatsu.reader.ui

import android.content.res.Configuration
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderFragment
import org.koitharu.kotatsu.reader.ui.pager.doublepage.DoubleReaderFragment
import org.koitharu.kotatsu.reader.ui.pager.reversed.ReversedReaderFragment
import org.koitharu.kotatsu.reader.ui.pager.standard.PagerReaderFragment
import org.koitharu.kotatsu.reader.ui.pager.vertical.VerticalReaderFragment
import org.koitharu.kotatsu.reader.ui.pager.webtoon.WebtoonReaderFragment
import java.util.EnumMap

class ReaderManager(
	private val fragmentManager: FragmentManager,
	private val container: FragmentContainerView,
	private val settings: AppSettings,
) {

	private val modeMap = EnumMap<ReaderMode, Class<out BaseReaderFragment<*>>>(ReaderMode::class.java)

	init {
		modeMap[ReaderMode.STANDARD] = if (useDoublePages()) {
			DoubleReaderFragment::class.java
		} else {
			PagerReaderFragment::class.java
		}
		modeMap[ReaderMode.REVERSED] = ReversedReaderFragment::class.java
		modeMap[ReaderMode.WEBTOON] = WebtoonReaderFragment::class.java
		modeMap[ReaderMode.VERTICAL] = VerticalReaderFragment::class.java
	}

	val currentReader: BaseReaderFragment<*>?
		get() = fragmentManager.findFragmentById(container.id) as? BaseReaderFragment<*>

	val currentMode: ReaderMode?
		get() {
			val readerClass = currentReader?.javaClass ?: return null
			return modeMap.entries.find { it.value == readerClass }?.key
		}

	fun replace(newMode: ReaderMode) {
		val readerClass = requireNotNull(modeMap[newMode])
		fragmentManager.commit {
			setReorderingAllowed(true)
			replace(container.id, readerClass, null, null)
		}
	}

	private fun useDoublePages() = container.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
		&& settings.isReaderDoubleOnLandscape

	/*fun replace(reader: BaseReaderFragment<*>) {
		fragmentManager.commit {
			setReorderingAllowed(true)
			replace(containerResId, reader)
		}
	}*/
}
