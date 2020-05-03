package org.koitharu.kotatsu.ui.common

import android.content.pm.PackageManager
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import moxy.MvpAppCompatActivity
import org.koin.core.KoinComponent
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R

abstract class BaseActivity : MvpAppCompatActivity(), KoinComponent {

	private var permissionCallback: ((Boolean) -> Unit)? = null

	override fun setContentView(layoutResID: Int) {
		super.setContentView(layoutResID)
		setupToolbar()
	}

	override fun setContentView(view: View?) {
		super.setContentView(view)
		setupToolbar()
	}

	private fun setupToolbar() {
		(findViewById<View>(R.id.toolbar) as? Toolbar)?.let(this::setSupportActionBar)
	}

	override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == android.R.id.home) {
		onBackPressed()
		true
	} else super.onOptionsItemSelected(item)

	fun requestPermission(permission: String, callback: (Boolean) -> Unit) {
		if (ContextCompat.checkSelfPermission(
				this,
				permission
			) == PackageManager.PERMISSION_GRANTED
		) {
			callback(true)
		} else {
			permissionCallback = callback
			ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_PERMISSION)
		}
	}

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<out String>,
		grantResults: IntArray
	) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == REQUEST_PERMISSION) {
			grantResults.singleOrNull()?.let {
				permissionCallback?.invoke(it == PackageManager.PERMISSION_GRANTED)
			}
			permissionCallback = null
		}
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		//TODO remove. Just for testing
		if (BuildConfig.DEBUG && keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			recreate()
			return true
		}
		if (BuildConfig.DEBUG && keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			throw StackOverflowError("test")
		}
		return super.onKeyDown(keyCode, event)
	}

	private companion object {

		const val REQUEST_PERMISSION = 30
	}
}