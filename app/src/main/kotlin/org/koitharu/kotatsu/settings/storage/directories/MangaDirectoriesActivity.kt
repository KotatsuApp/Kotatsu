package org.koitharu.kotatsu.settings.storage.directories

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.snackbar.Snackbar
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.os.OpenDocumentTreeHelper
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.core.util.ext.consumeAllSystemBarsInsets
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.tryLaunch
import org.koitharu.kotatsu.databinding.ActivityMangaDirectoriesBinding
import org.koitharu.kotatsu.settings.storage.RequestStorageManagerPermissionContract

@AndroidEntryPoint
class MangaDirectoriesActivity : BaseActivity<ActivityMangaDirectoriesBinding>(),
	OnListItemClickListener<DirectoryConfigModel>, View.OnClickListener {

	private val viewModel: MangaDirectoriesViewModel by viewModels()
	private val pickFileTreeLauncher = OpenDocumentTreeHelper(
		activityResultCaller = this,
		flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
			or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
			or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
	) {
		if (it != null) viewModel.onCustomDirectoryPicked(it)
	}
	private val permissionRequestLauncher = registerForActivityResult(
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			RequestStorageManagerPermissionContract()
		} else {
			ActivityResultContracts.RequestPermission()
		},
	) {
		if (it) {
			viewModel.updateList()
			if (!pickFileTreeLauncher.tryLaunch(null)) {
				Snackbar.make(
					viewBinding.recyclerView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT,
				).show()
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityMangaDirectoriesBinding.inflate(layoutInflater))
		setDisplayHomeAsUp(isEnabled = true, showUpAsClose = false)
		val adapter = AsyncListDifferDelegationAdapter(DirectoryConfigDiffCallback(), directoryConfigAD(this))
        val spacing = resources.getDimensionPixelOffset(R.dimen.list_spacing_large)
        viewBinding.recyclerView.adapter = adapter
        viewBinding.recyclerView.addItemDecoration(SpacingItemDecoration(spacing, withBottomPadding = false))
		viewBinding.fabAdd.setOnClickListener(this)
		viewModel.items.observe(this) { adapter.items = it }
		viewModel.isLoading.observe(this) { viewBinding.progressBar.isVisible = it }
		viewModel.onError.observeEvent(
			this,
			SnackbarErrorObserver(viewBinding.root, null, exceptionResolver) {
				if (it) viewModel.updateList()
			},
		)
	}

	override fun onItemClick(item: DirectoryConfigModel, view: View) {
		viewModel.onRemoveClick(item.path)
	}

	override fun onClick(v: View?) {
		if (!permissionRequestLauncher.tryLaunch(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			Snackbar.make(
				viewBinding.recyclerView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT,
			).show()
		}
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val barsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
		viewBinding.fabAdd.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			rightMargin = topMargin + barsInsets.right
			leftMargin = topMargin + barsInsets.left
			bottomMargin = topMargin + barsInsets.bottom
		}
		viewBinding.appbar.updatePadding(
			left = barsInsets.left,
			right = barsInsets.right,
			top = barsInsets.top,
		)
		viewBinding.recyclerView.updatePadding(
			left = barsInsets.left,
			right = barsInsets.right,
			bottom = barsInsets.bottom,
		)
		return insets.consumeAllSystemBarsInsets()
	}
}
