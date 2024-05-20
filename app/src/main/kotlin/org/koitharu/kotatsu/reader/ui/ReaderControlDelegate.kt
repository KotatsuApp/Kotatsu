package org.koitharu.kotatsu.reader.ui

import android.content.res.Resources
import android.view.KeyEvent
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.reader.data.TapGridSettings
import org.koitharu.kotatsu.reader.domain.TapGridArea
import org.koitharu.kotatsu.reader.ui.tapgrid.TapAction

class ReaderControlDelegate(
	resources: Resources,
	private val settings: AppSettings,
	private val tapGridSettings: TapGridSettings,
	private val listener: OnInteractionListener,
) {

	private var minScrollDelta = resources.getDimensionPixelSize(R.dimen.reader_scroll_delta_min)

	fun onGridTouch(area: TapGridArea): Boolean {
		val action = tapGridSettings.getTapAction(
			area = area,
			isLongTap = false,
		) ?: return false
		processAction(action)
		return true
	}

	fun onGridLongTouch(area: TapGridArea) {
		val action = tapGridSettings.getTapAction(
			area = area,
			isLongTap = true,
		) ?: return
		processAction(action)
	}

	fun onKeyDown(keyCode: Int): Boolean = when (keyCode) {

		KeyEvent.KEYCODE_R -> {
			listener.switchPageBy(1)
			true
		}

		KeyEvent.KEYCODE_L -> {
			listener.switchPageBy(-1)
			true
		}

		KeyEvent.KEYCODE_VOLUME_UP -> if (settings.isReaderVolumeButtonsEnabled) {
			listener.switchPageBy(-1)
			true
		} else {
			false
		}

		KeyEvent.KEYCODE_VOLUME_DOWN -> if (settings.isReaderVolumeButtonsEnabled) {
			listener.switchPageBy(1)
			true
		} else {
			false
		}

		KeyEvent.KEYCODE_SPACE,
		KeyEvent.KEYCODE_PAGE_DOWN,
		-> {
			listener.switchPageBy(1)
			true
		}

		KeyEvent.KEYCODE_DPAD_RIGHT -> {
			listener.switchPageBy(if (isReaderTapsReversed()) -1 else 1)
			true
		}

		KeyEvent.KEYCODE_PAGE_UP,
		-> {
			listener.switchPageBy(-1)
			true
		}

		KeyEvent.KEYCODE_DPAD_LEFT -> {
			listener.switchPageBy(if (isReaderTapsReversed()) 1 else -1)
			true
		}

		KeyEvent.KEYCODE_DPAD_CENTER -> {
			listener.toggleUiVisibility()
			true
		}

		KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP,
		KeyEvent.KEYCODE_DPAD_UP -> {
			if (!listener.scrollBy(-minScrollDelta, smooth = true)) {
				listener.switchPageBy(-1)
			}
			true
		}

		KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN,
		KeyEvent.KEYCODE_DPAD_DOWN -> {
			if (!listener.scrollBy(minScrollDelta, smooth = true)) {
				listener.switchPageBy(1)
			}
			true
		}

		else -> false
	}

	fun onKeyUp(keyCode: Int, @Suppress("UNUSED_PARAMETER") event: KeyEvent?): Boolean {
		return (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
			&& settings.isReaderVolumeButtonsEnabled
	}

	private fun processAction(action: TapAction) {
		when (action) {
			TapAction.PAGE_NEXT -> listener.switchPageBy(1)
			TapAction.PAGE_PREV -> listener.switchPageBy(-1)
			TapAction.CHAPTER_NEXT -> listener.switchChapterBy(1)
			TapAction.CHAPTER_PREV -> listener.switchChapterBy(-1)
			TapAction.TOGGLE_UI -> listener.toggleUiVisibility()
			TapAction.SHOW_MENU -> listener.openMenu()
		}
	}

	private fun isReaderTapsReversed(): Boolean {
		return settings.isReaderControlAlwaysLTR && listener.readerMode == ReaderMode.REVERSED
	}

	interface OnInteractionListener {

		val readerMode: ReaderMode?

		fun switchPageBy(delta: Int)

		fun switchChapterBy(delta: Int)

		fun scrollBy(delta: Int, smooth: Boolean): Boolean

		fun toggleUiVisibility()

		fun openMenu()

		fun isReaderResumed(): Boolean
	}
}
