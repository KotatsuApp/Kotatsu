package org.koitharu.kotatsu.settings.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.TooltipCompat
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
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
		arrayOf(
			binding.btn4pda,
			binding.btnDiscord,
			binding.btnGithub,
			binding.btnTelegram,
		).forEach { button ->
			TooltipCompat.setTooltipText(button, button.contentDescription)
			button.setOnClickListener(this)
		}
	}

	override fun onClick(v: View) {
		val urlResId = when (v.id) {
			R.id.btn_4pda -> R.string.url_forpda
			R.id.btn_discord -> R.string.url_discord
			R.id.btn_telegram -> R.string.url_telegram
			R.id.btn_github -> R.string.url_github
			else -> return
		}
		openLink(v.context.getString(urlResId), v.contentDescription)
	}

	private fun openLink(url: String, title: CharSequence?) {
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
		}
	}
}
