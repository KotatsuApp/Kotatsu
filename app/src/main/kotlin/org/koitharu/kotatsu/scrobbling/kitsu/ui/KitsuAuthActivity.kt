package org.koitharu.kotatsu.scrobbling.kitsu.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.graphics.Insets
import androidx.core.net.toUri
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.databinding.ActivityKitsuAuthBinding
import org.koitharu.kotatsu.parsers.util.urlEncoded

class KitsuAuthActivity : BaseActivity<ActivityKitsuAuthBinding>(), View.OnClickListener, TextWatcher {

	private val regexEmail = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", RegexOption.IGNORE_CASE)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityKitsuAuthBinding.inflate(layoutInflater))
		viewBinding.buttonCancel.setOnClickListener(this)
		viewBinding.buttonDone.setOnClickListener(this)
		viewBinding.editEmail.addTextChangedListener(this)
		viewBinding.editPassword.addTextChangedListener(this)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		val basePadding = resources.getDimensionPixelOffset(R.dimen.screen_padding)
		viewBinding.root.setPadding(
			basePadding + insets.left,
			basePadding + insets.top,
			basePadding + insets.right,
			basePadding + insets.bottom,
		)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_cancel -> finish()
			R.id.button_done -> continueAuth()
		}
	}

	override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

	override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

	override fun afterTextChanged(s: Editable?) {
		val email = viewBinding.editEmail.text?.toString()?.trim()
		val password = viewBinding.editPassword.text?.toString()?.trim()
		viewBinding.buttonDone.isEnabled = !email.isNullOrEmpty()
			&& !password.isNullOrEmpty()
			&& regexEmail.matches(email)
			&& password.length >= 3
	}

	private fun continueAuth() {
		val email = viewBinding.editEmail.text?.toString()?.trim().orEmpty()
		val password = viewBinding.editPassword.text?.toString()?.trim().orEmpty()
		val url = "kotatsu://kitsu-auth?code=" + "$email;$password".urlEncoded()
		val intent = Intent(Intent.ACTION_VIEW, url.toUri())
		startActivity(intent)
		finishAfterTransition()
	}
}
