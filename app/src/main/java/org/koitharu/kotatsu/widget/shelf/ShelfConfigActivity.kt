package org.koitharu.kotatsu.widget.shelf

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.R as materialR
import com.google.android.material.snackbar.Snackbar
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.prefs.AppWidgetConfig
import org.koitharu.kotatsu.databinding.ActivityCategoriesBinding
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.widget.shelf.adapter.CategorySelectAdapter
import org.koitharu.kotatsu.widget.shelf.model.CategoryItem

class ShelfConfigActivity :
	BaseActivity<ActivityCategoriesBinding>(),
	OnListItemClickListener<CategoryItem>,
	View.OnClickListener {

	private val viewModel by viewModels<ShelfConfigViewModel>()

	private lateinit var adapter: CategorySelectAdapter
	private lateinit var config: AppWidgetConfig

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityCategoriesBinding.inflate(layoutInflater))
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setHomeAsUpIndicator(materialR.drawable.abc_ic_clear_material)
		}
		adapter = CategorySelectAdapter(this)
		binding.recyclerView.adapter = adapter
		binding.buttonDone.isVisible = true
		binding.buttonDone.setOnClickListener(this)
		binding.fabAdd.hide()
		val appWidgetId = intent?.getIntExtra(
			AppWidgetManager.EXTRA_APPWIDGET_ID,
			AppWidgetManager.INVALID_APPWIDGET_ID,
		) ?: AppWidgetManager.INVALID_APPWIDGET_ID
		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finishAfterTransition()
			return
		}
		config = AppWidgetConfig(this, appWidgetId)
		viewModel.checkedId = config.categoryId

		viewModel.content.observe(this, this::onContentChanged)
		viewModel.onError.observe(this, this::onError)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_done -> {
				config.categoryId = viewModel.checkedId
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
		binding.fabAdd.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			rightMargin = topMargin + insets.right
			leftMargin = topMargin + insets.left
			bottomMargin = topMargin + insets.bottom
		}
		binding.recyclerView.updatePadding(
			left = insets.left,
			right = insets.right,
			bottom = insets.bottom,
		)
		with(binding.toolbar) {
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

	private fun onError(e: Throwable) {
		Snackbar.make(binding.recyclerView, e.getDisplayMessage(resources), Snackbar.LENGTH_LONG)
			.show()
	}

	private fun updateWidget() {
		val intent = Intent(this, ShelfWidgetProvider::class.java)
		intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
		val ids = intArrayOf(config.widgetId)
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
		sendBroadcast(intent)
	}
}
