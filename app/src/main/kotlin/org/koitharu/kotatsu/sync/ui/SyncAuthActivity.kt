package org.koitharu.kotatsu.sync.ui

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentResultListener
import androidx.transition.TransitionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.util.DefaultTextWatcher
import org.koitharu.kotatsu.core.util.ext.consumeAllSystemBarsInsets
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.getParcelableExtraCompat
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.systemBarsInsets
import org.koitharu.kotatsu.databinding.ActivitySyncAuthBinding
import org.koitharu.kotatsu.sync.data.SyncSettings
import org.koitharu.kotatsu.sync.domain.SyncAuthResult

private const val PAGE_EMAIL = 0
private const val PAGE_PASSWORD = 1
private const val PASSWORD_MIN_LENGTH = 4

@AndroidEntryPoint
class SyncAuthActivity : BaseActivity<ActivitySyncAuthBinding>(), View.OnClickListener, FragmentResultListener,
	DefaultTextWatcher {

	private var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null
	private var resultBundle: Bundle? = null
	private val pageBackCallback = PageBackCallback()

	private val regexEmail = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", RegexOption.IGNORE_CASE)

	private val viewModel by viewModels<SyncAuthViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySyncAuthBinding.inflate(layoutInflater))
		accountAuthenticatorResponse =
			intent.getParcelableExtraCompat(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
		accountAuthenticatorResponse?.onRequestContinued()
		viewBinding.buttonNext.setOnClickListener(this)
		viewBinding.buttonBack.setOnClickListener(this)
		viewBinding.buttonCancel.setOnClickListener(this)
		viewBinding.buttonDone.setOnClickListener(this)
		viewBinding.buttonSettings.setOnClickListener(this)
		viewBinding.editEmail.addTextChangedListener(this)
		viewBinding.editPassword.addTextChangedListener(this)

		onBackPressedDispatcher.addCallback(pageBackCallback)

		viewModel.onTokenObtained.observeEvent(this, ::onTokenReceived)
		viewModel.onError.observeEvent(this, ::onError)
		viewModel.isLoading.observe(this, ::onLoadingStateChanged)
		viewModel.onAccountAlreadyExists.observeEvent(this) {
			onAccountAlreadyExists()
		}

		supportFragmentManager.setFragmentResultListener(SyncHostDialogFragment.REQUEST_KEY, this, this)
		if (savedInstanceState == null) {
			setPage(PAGE_EMAIL)
		} else {
			pageBackCallback.update()
		}
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val barsInsets = insets.systemBarsInsets
		viewBinding.root.updatePadding(top = barsInsets.top)
		viewBinding.dockedToolbarChild.updateLayoutParams<MarginLayoutParams> {
			leftMargin = barsInsets.left
			rightMargin = barsInsets.right
			bottomMargin = barsInsets.bottom
		}
		val basePadding = viewBinding.layoutContent.paddingBottom
		viewBinding.layoutContent.updatePadding(
			left = barsInsets.left + basePadding,
			right = barsInsets.right + basePadding,
		)
		return insets.consumeAllSystemBarsInsets()
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_cancel -> {
				setResult(RESULT_CANCELED)
				finish()
			}

			R.id.button_next -> {
				setPage(PAGE_PASSWORD)
				viewBinding.editPassword.requestFocus()
			}

			R.id.button_back -> {
				setPage(PAGE_EMAIL)
				viewBinding.editEmail.requestFocus()
			}

			R.id.button_done -> {
				viewModel.obtainToken(
					email = viewBinding.editEmail.text.toString().trim(),
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

	override fun afterTextChanged(s: Editable?) {
		val isLoading = viewModel.isLoading.value
		val email = viewBinding.editEmail.text?.trim()?.toString()
		val password = viewBinding.editPassword.text?.toString()
		viewBinding.buttonNext.isEnabled = !isLoading && !email.isNullOrEmpty() && regexEmail.matches(email)
		viewBinding.buttonDone.isEnabled = !isLoading && password != null && password.length >= PASSWORD_MIN_LENGTH
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		with(viewBinding) {
			progressBar.isInvisible = !isLoading
			editEmail.isEnabled = !isLoading
			editPassword.isEnabled = !isLoading
		}
		afterTextChanged(null)
		pageBackCallback.update()
	}

	private fun setPage(page: Int) {
		with(viewBinding) {
			val currentPage = if (layoutEmail.isVisible) PAGE_EMAIL else PAGE_PASSWORD
			if (currentPage != page) {
				val transition = MaterialSharedAxis(MaterialSharedAxis.X, page > currentPage)
				TransitionManager.beginDelayedTransition(layoutContent, transition)
			}
			buttonNext.isVisible = page == PAGE_EMAIL
			buttonBack.isVisible = page == PAGE_PASSWORD
			buttonSettings.isVisible = page == PAGE_EMAIL
			buttonDone.isVisible = page == PAGE_PASSWORD
			buttonCancel.isVisible = page == PAGE_EMAIL
			layoutEmail.isVisible = page == PAGE_EMAIL
			layoutPassword.isVisible = page == PAGE_PASSWORD
		}
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

	private inner class PageBackCallback : OnBackPressedCallback(false) {

		override fun handleOnBackPressed() {
			setPage(PAGE_EMAIL)
			viewBinding.editEmail.requestFocus()
			update()
		}

		fun update() {
			isEnabled = !viewBinding.progressBar.isVisible && viewBinding.editPassword.isVisible
		}
	}
}
