package org.koitharu.kotatsu.ui.utils.protect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_protect.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.common.BaseActivity
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

class ProtectActivity : BaseActivity(), ProtectView, TextView.OnEditorActionListener, TextWatcher {

	private val presenter by moxyPresenter(factory = ::ProtectPresenter)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_protect)
		edit_password.setOnEditorActionListener(this)
		edit_password.addTextChangedListener(this)
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setHomeAsUpIndicator(R.drawable.ic_cross)
		}
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.opt_protect, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.action_done -> {
			presenter.tryUnlock(edit_password.text?.toString().orEmpty())
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
		return if (actionId == EditorInfo.IME_ACTION_DONE) {
			presenter.tryUnlock(edit_password.text?.toString().orEmpty())
			true
		} else {
			false
		}
	}

	override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

	override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

	override fun afterTextChanged(s: Editable?) {
		layout_password.error = null
	}

	override fun onUnlockSuccess() {
		AppProtectHelper.unlock(this)
	}

	override fun onError(e: Throwable) {
		layout_password.error = e.getDisplayMessage(resources)
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		layout_password.isEnabled = !isLoading
	}

	companion object {

		fun newIntent(context: Context) = Intent(context, ProtectActivity::class.java)
	}
}