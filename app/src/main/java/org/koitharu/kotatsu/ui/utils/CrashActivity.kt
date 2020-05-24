package org.koitharu.kotatsu.ui.utils

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_crash.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.list.MainActivity

class CrashActivity : Activity(), View.OnClickListener {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_crash)
		textView.text = intent.getStringExtra(Intent.EXTRA_TEXT)
		button_close.setOnClickListener(this)
		button_restart.setOnClickListener(this)
	}

	override fun onClick(v: View) {
		when(v.id) {
			R.id.button_close -> {
				finish()
			}
			R.id.button_restart -> {
				val intent = Intent(applicationContext, MainActivity::class.java)
				intent.flags = (Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
				startActivity(intent)
				finish()
			}
		}
	}
}