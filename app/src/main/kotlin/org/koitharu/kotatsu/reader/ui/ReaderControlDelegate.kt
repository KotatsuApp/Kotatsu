package org.koitharu.kotatsu.reader.ui

import android.content.SharedPreferences
import android.content.res.Resources
import android.view.KeyEvent
import android.view.SoundEffectConstants
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.core.util.GridTouchHelper

class ReaderControlDelegate(
	resources: Resources,
	private val settings: AppSettings,
	private val listener: OnInteractionListener,
	owner: LifecycleOwner,
) : DefaultLifecycleObserver, SharedPreferences.OnSharedPreferenceChangeListener {

	private var isTapSwitchEnabled: Boolean = true
	private var isVolumeKeysSwitchEnabled: Boolean = false
	private var isReaderTapsAdaptive: Boolean = true
	private var minScrollDelta = resources.getDimensionPixelSize(R.dimen.reader_scroll_delta_min)

	init {
		owner.lifecycle.addObserver(this)
		settings.subscribe(this)
		updateSettings()
	}

	override fun onDestroy(owner: LifecycleOwner) {
		settings.unsubscribe(this)
		owner.lifecycle.removeObserver(this)
		super.onDestroy(owner)
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		updateSettings()
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
				listener.switchPageBy(if (isReaderTapsReversed()) 1 else -1)
				view.playSoundEffect(SoundEffectConstants.NAVIGATION_LEFT)
			}

			GridTouchHelper.AREA_BOTTOM -> if (isTapSwitchEnabled) {
				listener.switchPageBy(1)
				view.playSoundEffect(SoundEffectConstants.NAVIGATION_DOWN)
			}

			GridTouchHelper.AREA_RIGHT -> if (isTapSwitchEnabled) {
				listener.switchPageBy(if (isReaderTapsReversed()) -1 else 1)
				view.playSoundEffect(SoundEffectConstants.NAVIGATION_RIGHT)
			}
		}
	}

	fun onKeyDown(keyCode: Int, @Suppress("UNUSED_PARAMETER") event: KeyEvent?): Boolean = when (keyCode) {
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
		return (
			isVolumeKeysSwitchEnabled &&
				(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
			)
	}

	private fun updateSettings() {
		val switch = settings.readerPageSwitch
		isTapSwitchEnabled = AppSettings.PAGE_SWITCH_TAPS in switch
		isVolumeKeysSwitchEnabled = AppSettings.PAGE_SWITCH_VOLUME_KEYS in switch
		isReaderTapsAdaptive = settings.isReaderTapsAdaptive
	}

	private fun isReaderTapsReversed(): Boolean {
		return isReaderTapsAdaptive && listener.readerMode == ReaderMode.REVERSED
	}

	interface OnInteractionListener {

		val readerMode: ReaderMode?

		fun switchPageBy(delta: Int)

		fun scrollBy(delta: Int, smooth: Boolean): Boolean

		fun toggleUiVisibility()

		fun isReaderResumed(): Boolean
	}
}
