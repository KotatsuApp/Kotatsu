package org.koitharu.kotatsu.sync.ui

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.databinding.ActivitySyncAuthBinding
import org.koitharu.kotatsu.sync.domain.SyncAuthResult
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

class SyncAuthActivity : BaseActivity<ActivitySyncAuthBinding>(), View.OnClickListener {

	private var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null
	private var resultBundle: Bundle? = null

	private val viewModel by viewModels<SyncAuthViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySyncAuthBinding.inflate(layoutInflater))
		accountAuthenticatorResponse = intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
		accountAuthenticatorResponse?.onRequestContinued()
		binding.buttonCancel.setOnClickListener(this)
		binding.buttonNext.setOnClickListener(this)
		binding.buttonBack.setOnClickListener(this)
		binding.buttonDone.setOnClickListener(this)
		binding.editEmail.addTextChangedListener(EmailTextWatcher(binding.buttonNext))
		binding.editPassword.addTextChangedListener(PasswordTextWatcher(binding.buttonDone))

		viewModel.onTokenObtained.observe(this, ::onTokenReceived)
		viewModel.onError.observe(this, ::onError)
		viewModel.isLoading.observe(this, ::onLoadingStateChanged)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		val basePadding = resources.getDimensionPixelOffset(R.dimen.screen_padding)
		binding.root.setPadding(
			basePadding + insets.left,
			basePadding + insets.top,
			basePadding + insets.right,
			basePadding + insets.bottom,
		)
	}

	override fun onBackPressed() {
		if (binding.switcher.isVisible && binding.switcher.displayedChild > 0) {
			binding.switcher.showPrevious()
		} else {
			super.onBackPressed()
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_cancel -> {
				setResult(RESULT_CANCELED)
				finish()
			}
			R.id.button_next -> {
				binding.switcher.showNext()
			}
			R.id.button_back -> {
				binding.switcher.showPrevious()
			}
			R.id.button_done -> {
				viewModel.obtainToken(
					email = binding.editEmail.text.toString(),
					password = binding.editPassword.text.toString(),
				)
			}
		}
	}

	override fun finish() {
		accountAuthenticatorResponse?.let { response ->
			resultBundle?.also {
				response.onResult(it)
			} ?: response.onError(AccountManager.ERROR_CODE_CANCELED, getString(R.string.canceled))
		}
		super.finish()
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		if (isLoading == binding.layoutProgress.isVisible) {
			return
		}
		TransitionManager.beginDelayedTransition(binding.root, Fade())
		binding.switcher.isGone = isLoading
		binding.layoutProgress.isVisible = isLoading
	}

	private fun onError(error: Throwable) {
		MaterialAlertDialogBuilder(this)
			.setTitle(R.string.error)
			.setMessage(error.getDisplayMessage(resources))
			.setNegativeButton(R.string.close, null)
			.show()
	}

	private fun onTokenReceived(authResult: SyncAuthResult) {
		val am = AccountManager.get(this)
		val account = Account(authResult.email, getString(R.string.account_type_sync))
		val result = Bundle()
		if (am.addAccountExplicitly(account, authResult.password, Bundle())) {
			result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
			result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
			result.putString(AccountManager.KEY_AUTHTOKEN, authResult.token)
			am.setAuthToken(account, account.type, authResult.token)
		} else {
			result.putString(AccountManager.KEY_ERROR_MESSAGE, getString(R.string.account_already_exists))
		}
		resultBundle = result
		setResult(RESULT_OK)
		finish()
	}

	private class EmailTextWatcher(
		private val button: Button,
	) : TextWatcher {

		private val regexEmail = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", RegexOption.IGNORE_CASE)

		override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

		override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

		override fun afterTextChanged(s: Editable?) {
			val text = s?.toString()
			button.isEnabled = !text.isNullOrEmpty() && regexEmail.matches(text)
		}
	}

	private class PasswordTextWatcher(
		private val button: Button,
	) : TextWatcher {

		override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

		override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

		override fun afterTextChanged(s: Editable?) {
			val text = s?.toString()
			button.isEnabled = text != null && text.length >= 4
		}
	}
}
