package org.koitharu.kotatsu.widget.recent

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppWidgetConfig
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.databinding.ActivityAppwidgetRecentBinding
import com.google.android.material.R as materialR

@AndroidEntryPoint
class RecentWidgetConfigActivity :
	BaseActivity<ActivityAppwidgetRecentBinding>(),
	View.OnClickListener {

	private lateinit var config: AppWidgetConfig

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityAppwidgetRecentBinding.inflate(layoutInflater))
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setHomeAsUpIndicator(materialR.drawable.abc_ic_clear_material)
		}
		viewBinding.buttonDone.setOnClickListener(this)
		val appWidgetId = intent?.getIntExtra(
			AppWidgetManager.EXTRA_APPWIDGET_ID,
			AppWidgetManager.INVALID_APPWIDGET_ID,
		) ?: AppWidgetManager.INVALID_APPWIDGET_ID
		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finishAfterTransition()
			return
		}
		config = AppWidgetConfig(this, RecentWidgetProvider::class.java, appWidgetId)
		viewBinding.switchBackground.isChecked = config.hasBackground
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_done -> {
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

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
			bottom = insets.bottom,
			top = insets.top,
		)
	}

	private fun updateWidget() {
		val intent = Intent(this, RecentWidgetProvider::class.java)
		intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
		val ids = intArrayOf(config.widgetId)
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
		sendBroadcast(intent)
	}
}
