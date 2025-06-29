package org.koitharu.kotatsu.reader.ui

import android.content.res.Resources
import android.view.KeyEvent
import android.view.View
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.reader.data.TapGridSettings
import org.koitharu.kotatsu.reader.domain.TapGridArea
import org.koitharu.kotatsu.reader.ui.tapgrid.TapAction
import kotlin.math.sign

class ReaderControlDelegate(
	resources: Resources,
	private val settings: AppSettings,
	private val tapGridSettings: TapGridSettings,
	private val listener: OnInteractionListener,
) : View.OnClickListener {

	private var minScrollDelta = resources.getDimensionPixelSize(R.dimen.reader_scroll_delta_min)

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_prev -> listener.switchChapterBy(-1)
			R.id.button_next -> listener.switchChapterBy(1)
		}
	}

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

	fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		when (keyCode) {
			KeyEvent.KEYCODE_NAVIGATE_NEXT,
			KeyEvent.KEYCODE_SPACE -> switchBy(1, event, false)

			KeyEvent.KEYCODE_PAGE_DOWN -> switchBy(1, event, false)


			KeyEvent.KEYCODE_NAVIGATE_PREVIOUS -> switchBy(-1, event, false)
			KeyEvent.KEYCODE_PAGE_UP -> switchBy(-1, event, false)

			KeyEvent.KEYCODE_R -> switchBy(1, null, false)

			KeyEvent.KEYCODE_L -> switchBy(-1, null, false)

			KeyEvent.KEYCODE_VOLUME_UP -> if (settings.isReaderVolumeButtonsEnabled) {
				switchBy(if (settings.isReaderNavigationInverted) 1 else -1, event, false)
			} else {
				return false
			}

			KeyEvent.KEYCODE_VOLUME_DOWN -> if (settings.isReaderVolumeButtonsEnabled) {
				switchBy(if (settings.isReaderNavigationInverted) -1 else 1, event, false)
			} else {
				return false
			}

			KeyEvent.KEYCODE_DPAD_RIGHT -> switchByRelative(if (settings.isReaderNavigationInverted) -1 else 1, event)

			KeyEvent.KEYCODE_DPAD_LEFT -> switchByRelative(if (settings.isReaderNavigationInverted) 1 else -1, event)

			KeyEvent.KEYCODE_DPAD_CENTER -> listener.toggleUiVisibility()

			KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP,
			KeyEvent.KEYCODE_DPAD_UP -> switchBy(if (settings.isReaderNavigationInverted) 1 else -1, event, true)

			KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN,
			KeyEvent.KEYCODE_DPAD_DOWN -> switchBy(if (settings.isReaderNavigationInverted) -1 else 1, event, true)

			else -> return false
		}
		return true
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

	private fun switchBy(delta: Int, event: KeyEvent?, scroll: Boolean) {
		if (event?.isCtrlPressed == true) {
			listener.switchChapterBy(delta)
		} else if (scroll) {
			if (!listener.scrollBy(minScrollDelta * delta.sign, smooth = true)) {
				listener.switchPageBy(delta)
			}
		} else {
			listener.switchPageBy(delta)
		}
	}

	private fun switchByRelative(delta: Int, event: KeyEvent?) {
		return switchBy(if (isReaderTapsReversed()) -delta else delta, event, scroll = false)
	}

	interface OnInteractionListener {

		val readerMode: ReaderMode?

		fun switchPageBy(delta: Int)

		fun switchPageTo(index: Int)

		fun switchChapterBy(delta: Int)

		fun scrollBy(delta: Int, smooth: Boolean): Boolean

		fun toggleUiVisibility()

		fun onBookmarkClick()

		fun openMenu()

		fun onSavePageClick()

		fun onScrollTimerClick(isLongClick: Boolean)

		fun toggleScreenOrientation()

		fun isReaderResumed(): Boolean
	}
}
