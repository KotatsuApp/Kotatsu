package org.koitharu.kotatsu.reader.ui

import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderFragment
import org.koitharu.kotatsu.reader.ui.pager.doublepage.DoublePageReaderFragment
import org.koitharu.kotatsu.reader.ui.pager.reversed.ReversedReaderFragment
import org.koitharu.kotatsu.reader.ui.pager.standard.PagerReaderFragment
import org.koitharu.kotatsu.reader.ui.pager.webtoon.WebtoonReaderFragment
import java.util.EnumMap

class ReaderManager(
	private val fragmentManager: FragmentManager,
	private val container: FragmentContainerView,
) {

	private val modeMap = EnumMap<ReaderMode, Class<out BaseReaderFragment<*>>>(ReaderMode::class.java)

	init {
		val isTablet = container.resources.getBoolean(R.bool.is_tablet)
		modeMap[ReaderMode.STANDARD] = if (isTablet) {
			DoublePageReaderFragment::class.java
		} else {
			PagerReaderFragment::class.java
		}
		modeMap[ReaderMode.REVERSED] = ReversedReaderFragment::class.java
		modeMap[ReaderMode.WEBTOON] = WebtoonReaderFragment::class.java
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

	/*fun replace(reader: BaseReaderFragment<*>) {
		fragmentManager.commit {
			setReorderingAllowed(true)
			replace(containerResId, reader)
		}
	}*/
}
