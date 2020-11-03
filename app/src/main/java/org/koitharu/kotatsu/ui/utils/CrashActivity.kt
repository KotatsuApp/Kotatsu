package org.koitharu.kotatsu.ui.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_crash.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.list.MainActivity
import org.koitharu.kotatsu.utils.ShareHelper

class CrashActivity : Activity(), View.OnClickListener {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_crash)
		textView.text = intent.getStringExtra(Intent.EXTRA_TEXT)
		button_close.setOnClickListener(this)
		button_restart.setOnClickListener(this)
		button_report.setOnClickListener(this)
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.opt_crash, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.action_share -> {
				ShareHelper.shareText(this, textView.text?.toString() ?: return false)
			}
			else -> return super.onOptionsItemSelected(item)
		}
		return true
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_close -> {
				finish()
			}
			R.id.button_restart -> {
				val intent = Intent(applicationContext, MainActivity::class.java)
				intent.flags = (Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
				startActivity(intent)
				finish()
			}
			R.id.button_report -> {
				val intent = Intent(Intent.ACTION_VIEW)
				intent.data = Uri.parse("https://github.com/nv95/Kotatsu/issues")
				try {
					startActivity(Intent.createChooser(intent, getString(R.string.report_github)))
				} catch (_: ActivityNotFoundException) {
				}
			}
		}
	}
}