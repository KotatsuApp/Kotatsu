package org.koitharu.kotatsu.main.ui.protect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.graphics.Insets
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.databinding.ActivityProtectBinding
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

class ProtectActivity : BaseActivity<ActivityProtectBinding>(), TextView.OnEditorActionListener,
	TextWatcher, View.OnClickListener {

	private val viewModel by viewModel<ProtectViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
		setContentView(ActivityProtectBinding.inflate(layoutInflater))
		binding.editPassword.setOnEditorActionListener(this)
		binding.editPassword.addTextChangedListener(this)
		binding.buttonNext.setOnClickListener(this)
		binding.buttonCancel.setOnClickListener(this)

		viewModel.onError.observe(this, this::onError)
		viewModel.isLoading.observe(this, this::onLoadingStateChanged)
		viewModel.onUnlockSuccess.observe(this) {
			val intent = intent.getParcelableExtra<Intent>(EXTRA_INTENT)
			startActivity(intent)
			finishAfterTransition()
		}

		binding.editPassword.requestFocus()
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		val basePadding = resources.getDimensionPixelOffset(R.dimen.screen_padding)
		binding.root.setPadding(
			basePadding + insets.left,
			basePadding + insets.top,
			basePadding + insets.right,
			basePadding + insets.bottom
		)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_next -> viewModel.tryUnlock(binding.editPassword.text?.toString().orEmpty())
			R.id.button_cancel -> finish()
		}
	}

	override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
		return if (actionId == EditorInfo.IME_ACTION_DONE && binding.buttonNext.isEnabled) {
			binding.buttonNext.performClick()
			true
		} else {
			false
		}
	}

	override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

	override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

	override fun afterTextChanged(s: Editable?) {
		binding.layoutPassword.error = null
		binding.buttonNext.isEnabled = !s.isNullOrEmpty()
	}

	private fun onError(e: Throwable) {
		binding.layoutPassword.error = e.getDisplayMessage(resources)
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		binding.layoutPassword.isEnabled = !isLoading
	}

	companion object {

		private const val EXTRA_INTENT = "src_intent"

		fun newIntent(context: Context, sourceIntent: Intent): Intent {
			return Intent(context, ProtectActivity::class.java)
				.putExtra(EXTRA_INTENT, sourceIntent)
		}
	}
}