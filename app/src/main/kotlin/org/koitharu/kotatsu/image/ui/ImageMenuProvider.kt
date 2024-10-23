package org.koitharu.kotatsu.image.ui

import android.Manifest
import android.os.Build
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import com.google.android.material.snackbar.Snackbar
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.isZipUri
import org.koitharu.kotatsu.core.util.ext.tryLaunch

class ImageMenuProvider(
	private val activity: ComponentActivity,
	private val snackbarHost: View,
	private val viewModel: ImageViewModel,
) : MenuProvider {

	private val permissionLauncher = activity.registerForActivityResult(
		ActivityResultContracts.RequestPermission(),
	) { isGranted ->
		if (isGranted) {
			saveImage()
		}
	}

	private val saveLauncher = activity.registerForActivityResult(
		ActivityResultContracts.CreateDocument("image/png"),
	) { uri ->
		if (uri != null) {
			viewModel.saveImage(uri)
		}
	}

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_image, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
		R.id.action_save -> {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
				permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
			} else {
				saveImage()
			}
			true
		}

		else -> false
	}

	private fun saveImage() {
		val name = activity.intent.data?.let {
			if (it.isZipUri()) {
				it.fragment
			} else {
				it.lastPathSegment
			}?.substringBeforeLast('.')?.plus(".png")
		}
		if (name == null || !saveLauncher.tryLaunch(name)) {
			Snackbar.make(snackbarHost, R.string.operation_not_supported, Snackbar.LENGTH_SHORT).show()
		}
	}
}
