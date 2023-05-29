package org.koitharu.kotatsu.settings.protect

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.CompoundButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.isGone
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.databinding.ActivitySetupProtectBinding

private const val MIN_PASSWORD_LENGTH = 4

@AndroidEntryPoint
class ProtectSetupActivity :
	BaseActivity<ActivitySetupProtectBinding>(),
	TextWatcher,
	View.OnClickListener,
	TextView.OnEditorActionListener,
	CompoundButton.OnCheckedChangeListener {

	private val viewModel by viewModels<ProtectSetupViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
		setContentView(ActivitySetupProtectBinding.inflate(layoutInflater))
		viewBinding.editPassword.addTextChangedListener(this)
		viewBinding.editPassword.setOnEditorActionListener(this)
		viewBinding.buttonNext.setOnClickListener(this)
		viewBinding.buttonCancel.setOnClickListener(this)

		viewBinding.switchBiometric.isChecked = viewModel.isBiometricEnabled
		viewBinding.switchBiometric.setOnCheckedChangeListener(this)

		viewModel.isSecondStep.observe(this, this::onStepChanged)
		viewModel.onPasswordSet.observeEvent(this) {
			finishAfterTransition()
		}
		viewModel.onPasswordMismatch.observeEvent(this) {
			viewBinding.editPassword.error = getString(R.string.passwords_mismatch)
		}
		viewModel.onClearText.observeEvent(this) {
			viewBinding.editPassword.text?.clear()
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
			R.id.button_cancel -> finish()
			R.id.button_next -> viewModel.onNextClick(
				password = viewBinding.editPassword.text?.toString() ?: return,
			)
		}
	}

	override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
		viewModel.setBiometricEnabled(isChecked)
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
		viewBinding.editPassword.error = null
		val isEnoughLength = (s?.length ?: 0) >= MIN_PASSWORD_LENGTH
		viewBinding.buttonNext.isEnabled = isEnoughLength
		viewBinding.layoutPassword.isHelperTextEnabled =
			!isEnoughLength || viewModel.isSecondStep.value == true
	}

	private fun onStepChanged(isSecondStep: Boolean) {
		viewBinding.buttonCancel.isGone = isSecondStep
		viewBinding.switchBiometric.isVisible = isSecondStep && isBiometricAvailable()
		if (isSecondStep) {
			viewBinding.layoutPassword.helperText = getString(R.string.repeat_password)
			viewBinding.buttonNext.setText(R.string.confirm)
		} else {
			viewBinding.layoutPassword.helperText = getString(R.string.password_length_hint)
			viewBinding.buttonNext.setText(R.string.next)
		}
	}

	private fun isBiometricAvailable(): Boolean {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
			packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
	}
}
