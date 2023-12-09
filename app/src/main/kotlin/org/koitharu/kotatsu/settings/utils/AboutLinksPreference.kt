package org.koitharu.kotatsu.settings.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.TooltipCompat
import androidx.core.net.toUri
import androidx.core.view.forEach
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.snackbar.Snackbar
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.PreferenceAboutLinksBinding

class AboutLinksPreference @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : Preference(context, attrs), View.OnClickListener {

	init {
		layoutResource = R.layout.preference_about_links
		isSelectable = false
		isPersistent = false
	}

	override fun onBindViewHolder(holder: PreferenceViewHolder) {
		super.onBindViewHolder(holder)

		val binding = PreferenceAboutLinksBinding.bind(holder.itemView)
		binding.root.forEach { button ->
			TooltipCompat.setTooltipText(button, button.contentDescription)
			button.setOnClickListener(this)
		}
	}

	override fun onClick(v: View) {
		val urlResId = when (v.id) {
			R.id.btn_discord -> R.string.url_discord
			R.id.btn_telegram -> R.string.url_telegram
			R.id.btn_github -> R.string.url_github
			else -> return
		}
		openLink(v, v.context.getString(urlResId), v.contentDescription)
	}

	private fun openLink(v: View, url: String, title: CharSequence?) {
		val intent = Intent(Intent.ACTION_VIEW, url.toUri())
		try {
			context.startActivity(
				if (title != null) {
					Intent.createChooser(intent, title)
				} else {
					intent
				},
			)
		} catch (_: ActivityNotFoundException) {
			Snackbar.make(v, R.string.operation_not_supported, Snackbar.LENGTH_SHORT).show()
		}
	}
}
