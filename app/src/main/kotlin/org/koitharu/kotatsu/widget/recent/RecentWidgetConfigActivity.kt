package org.koitharu.kotatsu.widget.recent

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppWidgetConfig
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.ext.consumeAllSystemBarsInsets
import org.koitharu.kotatsu.core.util.ext.systemBarsInsets
import org.koitharu.kotatsu.databinding.ActivityAppwidgetRecentBinding

@AndroidEntryPoint
class RecentWidgetConfigActivity :
	BaseActivity<ActivityAppwidgetRecentBinding>(),
	View.OnClickListener {

	private lateinit var config: AppWidgetConfig

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityAppwidgetRecentBinding.inflate(layoutInflater))
		setDisplayHomeAsUp(isEnabled = true, showUpAsClose = true)
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

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val barsInsets = insets.systemBarsInsets
		viewBinding.root.setPadding(
			barsInsets.left,
			barsInsets.top,
			barsInsets.right,
			barsInsets.bottom,
		)
		return insets.consumeAllSystemBarsInsets()
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_done -> {
				config.hasBackground = viewBinding.switchBackground.isChecked
				updateWidget()
				setResult(
					RESULT_OK,
					Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, config.widgetId),
				)
				finish()
			}
		}
	}

	private fun updateWidget() {
		val intent = Intent(this, RecentWidgetProvider::class.java)
		intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
		val ids = intArrayOf(config.widgetId)
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
		sendBroadcast(intent)
	}
}
