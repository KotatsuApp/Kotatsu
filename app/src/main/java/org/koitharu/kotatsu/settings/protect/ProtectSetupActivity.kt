package org.koitharu.kotatsu.settings.protect

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.graphics.Insets
import androidx.core.view.isGone
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.databinding.ActivitySetupProtectBinding

class ProtectSetupActivity : BaseActivity<ActivitySetupProtectBinding>(), TextWatcher,
	View.OnClickListener, TextView.OnEditorActionListener {

	private val viewModel by viewModel<ProtectSetupViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
		setContentView(ActivitySetupProtectBinding.inflate(layoutInflater))
		binding.editPassword.addTextChangedListener(this)
		binding.editPassword.setOnEditorActionListener(this)
		binding.buttonNext.setOnClickListener(this)
		binding.buttonCancel.setOnClickListener(this)

		viewModel.isSecondStep.observe(this, this::onStepChanged)
		viewModel.onPasswordSet.observe(this) {
			finishAfterTransition()
		}
		viewModel.onPasswordMismatch.observe(this) {
			binding.editPassword.error = getString(R.string.passwords_mismatch)
		}
		viewModel.onClearText.observe(this) {
			binding.editPassword.text?.clear()
		}
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
			R.id.button_cancel -> finish()
			R.id.button_next -> viewModel.onNextClick(
				password = binding.editPassword.text?.toString() ?: return
			)
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
		binding.editPassword.error = null
		val isEnoughLength = (s?.length ?: 0) >= MIN_PASSWORD_LENGTH
		binding.buttonNext.isEnabled = isEnoughLength
		binding.layoutPassword.isHelperTextEnabled =
			!isEnoughLength || viewModel.isSecondStep.value == true
	}

	private fun onStepChanged(isSecondStep: Boolean) {
		binding.buttonCancel.isGone = isSecondStep
		if (isSecondStep) {
			binding.layoutPassword.helperText = getString(R.string.repeat_password)
			binding.buttonNext.setText(R.string.confirm)
		} else {
			binding.layoutPassword.helperText = getString(R.string.password_length_hint)
			binding.buttonNext.setText(R.string.next)
		}
	}

	private companion object {

		const val MIN_PASSWORD_LENGTH = 4
	}
}