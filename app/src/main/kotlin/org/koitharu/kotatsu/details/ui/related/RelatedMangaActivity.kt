package org.koitharu.kotatsu.details.ui.related

import android.os.Bundle
import androidx.fragment.app.commit
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.databinding.ActivityContainerBinding
import org.koitharu.kotatsu.main.ui.owners.AppBarOwner

@AndroidEntryPoint
class RelatedMangaActivity : BaseActivity<ActivityContainerBinding>(), AppBarOwner {

	override val appBar: AppBarLayout
		get() = viewBinding.appbar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityContainerBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val fm = supportFragmentManager
		if (fm.findFragmentById(R.id.container) == null) {
			fm.commit {
				setReorderingAllowed(true)
				replace(R.id.container, RelatedListFragment::class.java, intent.extras)
			}
		}
	}
}
