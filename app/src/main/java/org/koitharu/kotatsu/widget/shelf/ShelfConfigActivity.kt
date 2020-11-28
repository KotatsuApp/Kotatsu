package org.koitharu.kotatsu.widget.shelf

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_categories.*
import org.koin.android.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.prefs.AppWidgetConfig
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.widget.shelf.adapter.CategorySelectAdapter
import org.koitharu.kotatsu.widget.shelf.model.CategoryItem

class ShelfConfigActivity : BaseActivity(), OnListItemClickListener<CategoryItem> {

	private val viewModel by viewModel<ShelfConfigViewModel>()

	private lateinit var adapter: CategorySelectAdapter
	private lateinit var config: AppWidgetConfig

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_categories)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		fab_add.imageTintList = ColorStateList.valueOf(Color.WHITE)
		adapter = CategorySelectAdapter(this)
		recyclerView.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
		recyclerView.adapter = adapter
		fab_add.isVisible = false
		val appWidgetId = intent?.getIntExtra(
			AppWidgetManager.EXTRA_APPWIDGET_ID,
			AppWidgetManager.INVALID_APPWIDGET_ID
		) ?: AppWidgetManager.INVALID_APPWIDGET_ID
		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finishAfterTransition()
			return
		}
		config = AppWidgetConfig.getInstance(this, appWidgetId)
		viewModel.checkedId = config.categoryId

		viewModel.content.observe(this, this::onContentChanged)
		viewModel.onError.observe(this, this::onError)
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.opt_config, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.action_done -> {
			config.categoryId = viewModel.checkedId
			updateWidget()
			setResult(
				Activity.RESULT_OK,
				Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, config.widgetId)
			)
			finish()
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onItemClick(item: CategoryItem, view: View) {
		viewModel.checkedId = item.id
	}

	private fun onContentChanged(categories: List<CategoryItem>) {
		adapter.items = categories
	}

	private fun onError(e: Throwable) {
		Snackbar.make(recyclerView, e.getDisplayMessage(resources), Snackbar.LENGTH_LONG)
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