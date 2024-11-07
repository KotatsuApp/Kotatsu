package org.koitharu.kotatsu.settings.sources.catalog

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import coil3.ImageLoader
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.titleResId
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.util.FadingAppbarMediator
import org.koitharu.kotatsu.core.ui.util.ReversibleActionObserver
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.ui.widgets.ChipsView.ChipModel
import org.koitharu.kotatsu.core.util.LocaleComparator
import org.koitharu.kotatsu.core.util.ext.getDisplayName
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.toLocale
import org.koitharu.kotatsu.databinding.ActivitySourcesCatalogBinding
import org.koitharu.kotatsu.list.ui.adapter.TypedListSpacingDecoration
import org.koitharu.kotatsu.main.ui.owners.AppBarOwner
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.search.ui.MangaListActivity
import javax.inject.Inject

@AndroidEntryPoint
class SourcesCatalogActivity : BaseActivity<ActivitySourcesCatalogBinding>(),
	OnListItemClickListener<SourceCatalogItem.Source>,
	AppBarOwner, MenuItem.OnActionExpandListener, ChipsView.OnChipClickListener {

	@Inject
	lateinit var coil: ImageLoader

	override val appBar: AppBarLayout
		get() = viewBinding.appbar

	private val viewModel by viewModels<SourcesCatalogViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySourcesCatalogBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val sourcesAdapter = SourcesCatalogAdapter(this, coil, this)
		with(viewBinding.recyclerView) {
			setHasFixedSize(true)
			addItemDecoration(TypedListSpacingDecoration(context, false))
			adapter = sourcesAdapter
		}
		viewBinding.chipsFilter.onChipClickListener = this
		FadingAppbarMediator(viewBinding.appbar, viewBinding.toolbar).bind()
		viewModel.content.observe(this, sourcesAdapter)
		viewModel.onActionDone.observeEvent(
			this,
			ReversibleActionObserver(viewBinding.recyclerView),
		)
		combine(viewModel.appliedFilter, viewModel.hasNewSources, viewModel.contentTypes, ::Triple).observe(this) {
			updateFilers(it.first, it.second, it.third)
		}
		addMenuProvider(SourcesCatalogMenuProvider(this, viewModel, this))
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		viewBinding.recyclerView.updatePadding(
			bottom = insets.bottom,
		)
	}

	override fun onChipClick(chip: Chip, data: Any?) {
		when (data) {
			is ContentType -> viewModel.setContentType(data, !chip.isChecked)
			is Boolean -> viewModel.setNewOnly(!chip.isChecked)
			else -> showLocalesMenu(chip)
		}
	}

	override fun onItemClick(item: SourceCatalogItem.Source, view: View) {
		startActivity(MangaListActivity.newIntent(this, item.source, null))
	}

	override fun onItemLongClick(item: SourceCatalogItem.Source, view: View): Boolean {
		viewModel.addSource(item.source)
		return false
	}

	override fun onMenuItemActionExpand(item: MenuItem): Boolean {
		val sq = (item.actionView as? SearchView)?.query?.trim()?.toString().orEmpty()
		viewModel.performSearch(sq)
		return true
	}

	override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
		viewModel.performSearch(null)
		return true
	}

	private fun updateFilers(
		appliedFilter: SourcesCatalogFilter,
		hasNewSources: Boolean,
		contentTypes: List<ContentType>,
	) {
		val chips = ArrayList<ChipModel>(contentTypes.size + 2)
		chips += ChipModel(
			title = appliedFilter.locale?.toLocale().getDisplayName(this),
			icon = R.drawable.ic_language,
			isDropdown = true,
		)
		if (hasNewSources) {
			chips += ChipModel(
				title = getString(R.string._new),
				icon = R.drawable.ic_updated,
				isChecked = appliedFilter.isNewOnly,
				data = true,
			)
		}
		contentTypes.mapTo(chips) { type ->
			ChipModel(
				title = getString(type.titleResId),
				isChecked = type in appliedFilter.types,
				data = type,
			)
		}
		viewBinding.chipsFilter.setChips(chips)
	}

	private fun showLocalesMenu(anchor: View) {
		val locales = viewModel.locales.mapTo(ArrayList(viewModel.locales.size)) {
			it to it?.toLocale()
		}
		locales.sortWith(compareBy(nullsFirst(LocaleComparator())) { it.second })
		val menu = PopupMenu(this, anchor)
		for ((i, lc) in locales.withIndex()) {
			menu.menu.add(Menu.NONE, Menu.NONE, i, lc.second.getDisplayName(this))
		}
		menu.setOnMenuItemClickListener {
			viewModel.setLocale(locales.getOrNull(it.order)?.first)
			true
		}
		menu.show()
	}
}
