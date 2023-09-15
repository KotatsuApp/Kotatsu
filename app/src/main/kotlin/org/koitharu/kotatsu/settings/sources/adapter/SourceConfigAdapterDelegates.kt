package org.koitharu.kotatsu.settings.sources.adapter

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.parser.favicon.faviconUri
import org.koitharu.kotatsu.core.ui.image.FaviconDrawable
import org.koitharu.kotatsu.core.ui.list.OnTipCloseListener
import org.koitharu.kotatsu.core.util.ext.crossfade
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.core.util.ext.source
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ItemExpandableBinding
import org.koitharu.kotatsu.databinding.ItemFilterHeaderBinding
import org.koitharu.kotatsu.databinding.ItemSourceConfigBinding
import org.koitharu.kotatsu.databinding.ItemSourceConfigCheckableBinding
import org.koitharu.kotatsu.databinding.ItemTipBinding
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem

fun sourceConfigHeaderDelegate() =
	adapterDelegateViewBinding<SourceConfigItem.Header, SourceConfigItem, ItemFilterHeaderBinding>(
		{ layoutInflater, parent ->
			ItemFilterHeaderBinding.inflate(
				layoutInflater,
				parent,
				false,
			)
		},
	) {

		bind {
			binding.textViewTitle.setText(item.titleResId)
		}
	}

fun sourceConfigGroupDelegate(
	listener: SourceConfigListener,
) =
	adapterDelegateViewBinding<SourceConfigItem.LocaleGroup, SourceConfigItem, ItemExpandableBinding>(
		{ layoutInflater, parent -> ItemExpandableBinding.inflate(layoutInflater, parent, false) },
	) {

		binding.root.setOnClickListener {
			listener.onHeaderClick(item)
		}

		bind {
			binding.root.text = item.title ?: getString(R.string.various_languages)
			binding.root.isChecked = item.isExpanded
		}
	}

fun sourceConfigItemCheckableDelegate(
	listener: SourceConfigListener,
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) =
	adapterDelegateViewBinding<SourceConfigItem.SourceItem, SourceConfigItem, ItemSourceConfigCheckableBinding>(
		{ layoutInflater, parent ->
			ItemSourceConfigCheckableBinding.inflate(
				layoutInflater,
				parent,
				false,
			)
		},
	) {

		binding.switchToggle.setOnCheckedChangeListener { _, isChecked ->
			listener.onItemEnabledChanged(item, isChecked)
		}

		bind {
			binding.textViewTitle.text = if (item.isNsfw) {
				buildSpannedString {
					append(item.source.title)
					append(' ')
					appendNsfwLabel(context)
				}
			} else {
				item.source.title
			}
			binding.switchToggle.isChecked = item.isEnabled
			binding.switchToggle.isEnabled = item.isAvailable
			binding.textViewDescription.textAndVisible = item.summary
			val fallbackIcon =
				FaviconDrawable(context, R.style.FaviconDrawable_Small, item.source.name)
			binding.imageViewIcon.newImageRequest(lifecycleOwner, item.source.faviconUri())?.run {
				crossfade(context)
				error(fallbackIcon)
				placeholder(fallbackIcon)
				fallback(fallbackIcon)
				source(item.source)
				enqueueWith(coil)
			}
		}
	}

fun sourceConfigItemDelegate2(
	listener: SourceConfigListener,
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) =
	adapterDelegateViewBinding<SourceConfigItem.SourceItem, SourceConfigItem, ItemSourceConfigBinding>(
		{ layoutInflater, parent ->
			ItemSourceConfigBinding.inflate(
				layoutInflater,
				parent,
				false,
			)
		},
	) {

		val eventListener = View.OnClickListener { v ->
			when (v.id) {
				R.id.imageView_add -> listener.onItemEnabledChanged(item, true)
				R.id.imageView_remove -> listener.onItemEnabledChanged(item, false)
				R.id.imageView_menu -> showSourceMenu(v, item, listener)
			}
		}
		binding.imageViewRemove.setOnClickListener(eventListener)
		binding.imageViewAdd.setOnClickListener(eventListener)
		binding.imageViewMenu.setOnClickListener(eventListener)

		bind {
			binding.textViewTitle.text = if (item.isNsfw) {
				buildSpannedString {
					append(item.source.title)
					append(' ')
					appendNsfwLabel(context)
				}
			} else {
				item.source.title
			}
			binding.imageViewAdd.isGone = item.isEnabled || !item.isAvailable
			binding.imageViewRemove.isVisible = item.isEnabled
			binding.imageViewMenu.isVisible = item.isEnabled
			binding.textViewDescription.textAndVisible = item.summary
			val fallbackIcon =
				FaviconDrawable(context, R.style.FaviconDrawable_Small, item.source.name)
			binding.imageViewIcon.newImageRequest(lifecycleOwner, item.source.faviconUri())?.run {
				crossfade(context)
				error(fallbackIcon)
				placeholder(fallbackIcon)
				fallback(fallbackIcon)
				source(item.source)
				enqueueWith(coil)
			}
		}
	}

fun sourceConfigTipDelegate(
	listener: OnTipCloseListener<SourceConfigItem.Tip>,
) = adapterDelegateViewBinding<SourceConfigItem.Tip, SourceConfigItem, ItemTipBinding>(
	{ layoutInflater, parent -> ItemTipBinding.inflate(layoutInflater, parent, false) },
) {

	binding.buttonClose.setOnClickListener {
		listener.onCloseTip(item)
	}

	bind {
		binding.imageViewIcon.setImageResource(item.iconResId)
		binding.textView.setText(item.textResId)
	}
}

fun sourceConfigEmptySearchDelegate() =
	adapterDelegate<SourceConfigItem.EmptySearchResult, SourceConfigItem>(
		R.layout.item_sources_empty,
	) { }

fun SpannableStringBuilder.appendNsfwLabel(context: Context) = inSpans(
	ForegroundColorSpan(
		context.getThemeColor(
			com.google.android.material.R.attr.colorError,
			Color.RED,
		),
	),
	RelativeSizeSpan(0.74f),
	SuperscriptSpan(),
) {
	append(context.getString(R.string.nsfw))
}

private fun showSourceMenu(
	anchor: View,
	item: SourceConfigItem.SourceItem,
	listener: SourceConfigListener,
) {
	val menu = PopupMenu(anchor.context, anchor)
	menu.inflate(R.menu.popup_source_config)
	menu.menu.findItem(R.id.action_shortcut)
		?.isVisible = ShortcutManagerCompat.isRequestPinShortcutSupported(anchor.context)
	menu.setOnMenuItemClickListener {
		when (it.itemId) {
			R.id.action_settings -> listener.onItemSettingsClick(item)
			R.id.action_lift -> listener.onItemLiftClick(item)
			R.id.action_shortcut -> listener.onItemShortcutClick(item)
		}
		true
	}
	menu.show()
}
