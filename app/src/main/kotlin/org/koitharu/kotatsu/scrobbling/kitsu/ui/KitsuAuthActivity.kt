package org.koitharu.kotatsu.scrobbling.kitsu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.graphics.Insets
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.databinding.ActivityKitsuAuthBinding

class KitsuAuthActivity : BaseActivity<ActivityKitsuAuthBinding>() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityKitsuAuthBinding.inflate(layoutInflater))
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

	companion object {
		fun newIntent(context: Context) = Intent(context, KitsuAuthActivity::class.java)
	}

}
