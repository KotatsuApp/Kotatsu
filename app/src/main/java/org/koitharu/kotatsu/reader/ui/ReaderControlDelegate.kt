package org.koitharu.kotatsu.reader.ui

import android.view.KeyEvent
import android.view.SoundEffectConstants
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.utils.GridTouchHelper

@Suppress("UNUSED_PARAMETER")
class ReaderControlDelegate(
	private val scope: LifecycleCoroutineScope,
	private val settings: AppSettings,
	private val listener: OnInteractionListener
) {

	private var isTapSwitchEnabled: Boolean = true
	private var isVolumeKeysSwitchEnabled: Boolean = false

	init {
		settings.observe()
			.filter { it == AppSettings.KEY_READER_SWITCHERS }
			.map { settings.readerPageSwitch }
			.onStart { emit(settings.readerPageSwitch) }
			.distinctUntilChanged()
			.flowOn(Dispatchers.IO)
			.onEach {
				isTapSwitchEnabled = AppSettings.PAGE_SWITCH_TAPS in it
				isVolumeKeysSwitchEnabled = AppSettings.PAGE_SWITCH_VOLUME_KEYS in it
			}.launchIn(scope)
	}

	fun onGridTouch(area: Int, view: View) {
		when (area) {
			GridTouchHelper.AREA_CENTER -> {
				listener.toggleUiVisibility()
				view.playSoundEffect(SoundEffectConstants.CLICK)
			}
			GridTouchHelper.AREA_TOP -> if (isTapSwitchEnabled) {
				listener.switchPageBy(-1)
				view.playSoundEffect(SoundEffectConstants.NAVIGATION_UP)
			}
			GridTouchHelper.AREA_LEFT -> if (isTapSwitchEnabled) {
				listener.switchPageBy(-1)
				view.playSoundEffect(SoundEffectConstants.NAVIGATION_LEFT)
			}
			GridTouchHelper.AREA_BOTTOM -> if (isTapSwitchEnabled) {
				listener.switchPageBy(1)
				view.playSoundEffect(SoundEffectConstants.NAVIGATION_DOWN)
			}
			GridTouchHelper.AREA_RIGHT -> if (isTapSwitchEnabled) {
				listener.switchPageBy(1)
				view.playSoundEffect(SoundEffectConstants.NAVIGATION_RIGHT)
			}
		}
	}

	fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = when (keyCode) {
		KeyEvent.KEYCODE_VOLUME_UP -> if (isVolumeKeysSwitchEnabled) {
			listener.switchPageBy(-1)
			true
		} else {
			false
		}
		KeyEvent.KEYCODE_VOLUME_DOWN -> if (isVolumeKeysSwitchEnabled) {
			listener.switchPageBy(1)
			true
		} else {
			false
		}
		KeyEvent.KEYCODE_SPACE,
		KeyEvent.KEYCODE_PAGE_DOWN,
		KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN,
		KeyEvent.KEYCODE_DPAD_DOWN,
		KeyEvent.KEYCODE_DPAD_RIGHT -> {
			listener.switchPageBy(1)
			true
		}
		KeyEvent.KEYCODE_PAGE_UP,
		KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP,
		KeyEvent.KEYCODE_DPAD_UP,
		KeyEvent.KEYCODE_DPAD_LEFT -> {
			listener.switchPageBy(-1)
			true
		}
		KeyEvent.KEYCODE_DPAD_CENTER -> {
			listener.toggleUiVisibility()
			true
		}
		else -> false
	}

	fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
		return (isVolumeKeysSwitchEnabled &&
				(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP))
	}

	interface OnInteractionListener {

		fun switchPageBy(delta: Int)

		fun toggleUiVisibility()
	}
}