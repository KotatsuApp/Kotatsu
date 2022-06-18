package org.koitharu.kotatsu.settings.utils

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.utils.ext.setTooltip

class AboutLinksPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
	Preference(context, attrs) {

	init {
		layoutResource = R.layout.preference_about_links
		isSelectable = false
	}

	override fun onBindViewHolder(holder: PreferenceViewHolder) {
		super.onBindViewHolder(holder)

		holder.findViewById(R.id.btn_4pda).apply {
			setTooltip(contentDescription.toString())
			setOnClickListener { openLink(resources.getString(R.string.url_forpda), contentDescription.toString()) }
		}
		holder.findViewById(R.id.btn_discord).apply {
			setTooltip(contentDescription.toString())
			setOnClickListener { openLink(resources.getString(R.string.url_discord), contentDescription.toString()) }
		}
		holder.findViewById(R.id.btn_twitter).apply {
			setTooltip(contentDescription.toString())
			setOnClickListener { openLink(resources.getString(R.string.url_twitter), contentDescription.toString()) }
		}
		holder.findViewById(R.id.btn_reddit).apply {
			setTooltip(contentDescription.toString())
			setOnClickListener { openLink(resources.getString(R.string.url_reddit), contentDescription.toString()) }
		}
		holder.findViewById(R.id.btn_github).apply {
			setTooltip(contentDescription.toString())
			setOnClickListener {
				openLink(
					resources.getString(R.string.url_github),
					contentDescription.toString()
				)
			}
		}
	}

	private fun openLink(url: String, title: CharSequence?) {
		val intent = Intent(Intent.ACTION_VIEW)
		intent.data = url.toUri()
		context.startActivity(
			if (title != null) {
				Intent.createChooser(intent, title)
			} else {
				intent
			}
		)
	}
}