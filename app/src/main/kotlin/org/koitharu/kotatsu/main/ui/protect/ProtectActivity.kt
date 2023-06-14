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
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.core.graphics.Insets
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.getParcelableExtraCompat
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.databinding.ActivityProtectBinding

@AndroidEntryPoint
class ProtectActivity :
	BaseActivity<ActivityProtectBinding>(),
	TextView.OnEditorActionListener,
	TextWatcher,
	View.OnClickListener {

	private val viewModel by viewModels<ProtectViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
		setContentView(ActivityProtectBinding.inflate(layoutInflater))
		viewBinding.editPassword.setOnEditorActionListener(this)
		viewBinding.editPassword.addTextChangedListener(this)
		viewBinding.buttonNext.setOnClickListener(this)
		viewBinding.buttonCancel.setOnClickListener(this)

		viewModel.onError.observeEvent(this, this::onError)
		viewModel.isLoading.observe(this, this::onLoadingStateChanged)
		viewModel.onUnlockSuccess.observeEvent(this) {
			val intent = intent.getParcelableExtraCompat<Intent>(EXTRA_INTENT)
			startActivity(intent)
			finishAfterTransition()
		}
	}

	override fun onStart() {
		super.onStart()
		if (!useFingerprint()) {
			viewBinding.editPassword.requestFocus()
		}
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
			R.id.button_next -> viewModel.tryUnlock(viewBinding.editPassword.text?.toString().orEmpty())
			R.id.button_cancel -> finish()
		}
	}

	override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
		return if (actionId == EditorInfo.IME_ACTION_DONE && viewBinding.buttonNext.isEnabled) {
			viewBinding.buttonNext.performClick()
			true
		} else {
			false
		}
	}

	override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

	override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

	override fun afterTextChanged(s: Editable?) {
		viewBinding.layoutPassword.error = null
		viewBinding.buttonNext.isEnabled = !s.isNullOrEmpty()
	}

	private fun onError(e: Throwable) {
		viewBinding.layoutPassword.error = e.getDisplayMessage(resources)
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		viewBinding.layoutPassword.isEnabled = !isLoading
	}

	private fun useFingerprint(): Boolean {
		if (!viewModel.isBiometricEnabled) {
			return false
		}
		if (BiometricManager.from(this).canAuthenticate(BIOMETRIC_WEAK) != BIOMETRIC_SUCCESS) {
			return false
		}
		val prompt = BiometricPrompt(this, BiometricCallback())
		val promptInfo = BiometricPrompt.PromptInfo.Builder()
			.setAllowedAuthenticators(BIOMETRIC_WEAK)
			.setTitle(getString(R.string.app_name))
			.setConfirmationRequired(false)
			.setNegativeButtonText(getString(android.R.string.cancel))
			.build()
		prompt.authenticate(promptInfo)
		return true
	}

	private inner class BiometricCallback : AuthenticationCallback() {
		override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
			super.onAuthenticationSucceeded(result)
			viewModel.unlock()
		}
	}

	companion object {

		private const val EXTRA_INTENT = "src_intent"

		fun newIntent(context: Context, sourceIntent: Intent): Intent {
			return Intent(context, ProtectActivity::class.java)
				.putExtra(EXTRA_INTENT, sourceIntent)
		}
	}
}
