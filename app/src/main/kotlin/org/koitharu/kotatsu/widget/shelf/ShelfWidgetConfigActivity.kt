package org.koitharu.kotatsu.widget.shelf

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.prefs.AppWidgetConfig
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.databinding.ActivityAppwidgetShelfBinding
import org.koitharu.kotatsu.widget.shelf.adapter.CategorySelectAdapter
import org.koitharu.kotatsu.widget.shelf.model.CategoryItem
import com.google.android.material.R as materialR

@AndroidEntryPoint
class ShelfWidgetConfigActivity :
	BaseActivity<ActivityAppwidgetShelfBinding>(),
	OnListItemClickListener<CategoryItem>,
	View.OnClickListener {

	private val viewModel by viewModels<ShelfConfigViewModel>()

	private lateinit var adapter: CategorySelectAdapter
	private lateinit var config: AppWidgetConfig

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityAppwidgetShelfBinding.inflate(layoutInflater))
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setHomeAsUpIndicator(materialR.drawable.abc_ic_clear_material)
		}
		adapter = CategorySelectAdapter(this)
		viewBinding.recyclerView.adapter = adapter
		viewBinding.buttonDone.setOnClickListener(this)
		val appWidgetId = intent?.getIntExtra(
			AppWidgetManager.EXTRA_APPWIDGET_ID,
			AppWidgetManager.INVALID_APPWIDGET_ID,
		) ?: AppWidgetManager.INVALID_APPWIDGET_ID
		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finishAfterTransition()
			return
		}
		config = AppWidgetConfig(this, ShelfWidgetProvider::class.java, appWidgetId)
		viewModel.checkedId = config.categoryId
		viewBinding.switchBackground.isChecked = config.hasBackground

		viewModel.content.observe(this, this::onContentChanged)
		viewModel.onError.observeEvent(this, SnackbarErrorObserver(viewBinding.recyclerView, null))
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_done -> {
				config.categoryId = viewModel.checkedId
				config.hasBackground = viewBinding.switchBackground.isChecked
				updateWidget()
				setResult(
					Activity.RESULT_OK,
					Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, config.widgetId),
				)
				finish()
			}
		}
	}

	override fun onItemClick(item: CategoryItem, view: View) {
		viewModel.checkedId = item.id
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.recyclerView.updatePadding(
			left = insets.left,
			right = insets.right,
			bottom = insets.bottom,
		)
		with(viewBinding.toolbar) {
			updatePadding(
				left = insets.left,
				right = insets.right,
			)
			updateLayoutParams<ViewGroup.MarginLayoutParams> {
				topMargin = insets.top
			}
		}
	}

	private fun onContentChanged(categories: List<CategoryItem>) {
		adapter.items = categories
	}

	private fun updateWidget() {
		val intent = Intent(this, ShelfWidgetProvider::class.java)
		intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
		val ids = intArrayOf(config.widgetId)
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
		sendBroadcast(intent)
	}
}
