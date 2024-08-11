package org.koitharu.kotatsu.sync.ui

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentResultListener
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.getParcelableExtraCompat
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.databinding.ActivitySyncAuthBinding
import org.koitharu.kotatsu.sync.data.SyncSettings
import org.koitharu.kotatsu.sync.domain.SyncAuthResult

@AndroidEntryPoint
class SyncAuthActivity : BaseActivity<ActivitySyncAuthBinding>(), View.OnClickListener, FragmentResultListener {

	private var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null
	private var resultBundle: Bundle? = null
	private val pageBackCallback = PageBackCallback()

	private val viewModel by viewModels<SyncAuthViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySyncAuthBinding.inflate(layoutInflater))
		accountAuthenticatorResponse =
			intent.getParcelableExtraCompat(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
		accountAuthenticatorResponse?.onRequestContinued()
		viewBinding.buttonCancel.setOnClickListener(this)
		viewBinding.buttonNext.setOnClickListener(this)
		viewBinding.buttonBack.setOnClickListener(this)
		viewBinding.buttonDone.setOnClickListener(this)
		viewBinding.layoutProgress.setOnClickListener(this)
		viewBinding.buttonSettings.setOnClickListener(this)
		viewBinding.editEmail.addTextChangedListener(EmailTextWatcher(viewBinding.buttonNext))
		viewBinding.editPassword.addTextChangedListener(PasswordTextWatcher(viewBinding.buttonDone))

		onBackPressedDispatcher.addCallback(pageBackCallback)

		viewModel.onTokenObtained.observeEvent(this, ::onTokenReceived)
		viewModel.onError.observeEvent(this, ::onError)
		viewModel.isLoading.observe(this, ::onLoadingStateChanged)
		viewModel.onAccountAlreadyExists.observeEvent(this) {
			onAccountAlreadyExists()
		}

		supportFragmentManager.setFragmentResultListener(SyncHostDialogFragment.REQUEST_KEY, this, this)
		pageBackCallback.update()
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
			R.id.button_cancel -> {
				setResult(RESULT_CANCELED)
				finish()
			}

			R.id.button_next -> {
				viewBinding.groupLogin.isVisible = false
				viewBinding.groupPassword.isVisible = true
				pageBackCallback.update()
				viewBinding.editPassword.requestFocus()
			}

			R.id.button_back -> {
				viewBinding.groupPassword.isVisible = false
				viewBinding.groupLogin.isVisible = true
				pageBackCallback.update()
				viewBinding.editEmail.requestFocus()
			}

			R.id.button_done -> {
				viewModel.obtainToken(
					email = viewBinding.editEmail.text.toString(),
					password = viewBinding.editPassword.text.toString(),
				)
			}

			R.id.button_settings -> {
				SyncHostDialogFragment.show(supportFragmentManager, viewModel.syncURL.value)
			}
		}
	}

	override fun onFragmentResult(requestKey: String, result: Bundle) {
		val syncURL = result.getString(SyncHostDialogFragment.KEY_SYNC_URL) ?: return
		viewModel.syncURL.value = syncURL
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
		if (isLoading == viewBinding.layoutProgress.isVisible) {
			return
		}
		TransitionManager.beginDelayedTransition(viewBinding.root, Fade())
		viewBinding.layoutProgress.isVisible = isLoading
		pageBackCallback.update()
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
		val userdata = Bundle(1)
		userdata.putString(SyncSettings.KEY_SYNC_URL, authResult.syncURL)
		val result = Bundle()
		if (am.addAccountExplicitly(account, authResult.password, userdata)) {
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

	private fun onAccountAlreadyExists() {
		Toast.makeText(this, R.string.account_already_exists, Toast.LENGTH_SHORT)
			.show()
		accountAuthenticatorResponse?.onError(
			AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION,
			getString(R.string.account_already_exists),
		)
		super.finishAfterTransition()
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

	private inner class PageBackCallback : OnBackPressedCallback(false) {

		override fun handleOnBackPressed() {
			viewBinding.groupLogin.isVisible = true
			viewBinding.groupPassword.isVisible = false
			viewBinding.editEmail.requestFocus()
			update()
		}

		fun update() {
			isEnabled = !viewBinding.layoutProgress.isVisible && viewBinding.groupPassword.isVisible
		}
	}
}
