package org.koitharu.kotatsu.reader.ui

import androidx.annotation.IdRes
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderFragment
import org.koitharu.kotatsu.reader.ui.pager.reversed.ReversedReaderFragment
import org.koitharu.kotatsu.reader.ui.pager.standard.PagerReaderFragment
import org.koitharu.kotatsu.reader.ui.pager.webtoon.WebtoonReaderFragment
import java.util.EnumMap

class ReaderManager(
	private val fragmentManager: FragmentManager,
	@IdRes private val containerResId: Int,
) {

	private val modeMap = EnumMap<ReaderMode, Class<out BaseReaderFragment<*>>>(ReaderMode::class.java)

	init {
		modeMap[ReaderMode.STANDARD] = PagerReaderFragment::class.java
		modeMap[ReaderMode.REVERSED] = ReversedReaderFragment::class.java
		modeMap[ReaderMode.WEBTOON] = WebtoonReaderFragment::class.java
	}

	val currentReader: BaseReaderFragment<*>?
		get() = fragmentManager.findFragmentById(containerResId) as? BaseReaderFragment<*>

	val currentMode: ReaderMode?
		get() {
			val readerClass = currentReader?.javaClass ?: return null
			return modeMap.entries.find { it.value == readerClass }?.key
		}

	fun replace(newMode: ReaderMode) {
		val readerClass = requireNotNull(modeMap[newMode])
		fragmentManager.commit {
			setReorderingAllowed(true)
			replace(containerResId, readerClass, null, null)
		}
	}

	fun replace(reader: BaseReaderFragment<*>) {
		fragmentManager.commit {
			setReorderingAllowed(true)
			replace(containerResId, reader)
		}
	}
}
