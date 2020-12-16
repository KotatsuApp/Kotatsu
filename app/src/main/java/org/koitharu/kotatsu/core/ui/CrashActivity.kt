package org.koitharu.kotatsu.core.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.ActivityCrashBinding
import org.koitharu.kotatsu.main.ui.MainActivity
import org.koitharu.kotatsu.utils.ShareHelper

class CrashActivity : Activity(), View.OnClickListener {

	private lateinit var binding: ActivityCrashBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityCrashBinding.inflate(layoutInflater)
		setContentView(binding.root)
		binding.textView.text = intent.getStringExtra(Intent.EXTRA_TEXT)
		binding.buttonClose.setOnClickListener(this)
		binding.buttonRestart.setOnClickListener(this)
		binding.buttonReport.setOnClickListener(this)
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.opt_crash, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.action_share -> {
				ShareHelper(this).shareText(binding.textView.text.toString())
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