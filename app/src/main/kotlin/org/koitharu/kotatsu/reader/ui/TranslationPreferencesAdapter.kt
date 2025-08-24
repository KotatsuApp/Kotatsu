package org.koitharu.kotatsu.reader.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.databinding.ItemTranslationPreferenceBinding
import org.koitharu.kotatsu.reader.domain.MangaTranslationPreference

class TranslationPreferencesAdapter(
	private val clickListener: OnListItemClickListener<MangaTranslationPreference>
) : ListAdapter<MangaTranslationPreference, TranslationPreferencesAdapter.ViewHolder>(DiffCallback()) {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val binding = ItemTranslationPreferenceBinding.inflate(
			LayoutInflater.from(parent.context), parent, false
		)
		return ViewHolder(binding, clickListener)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

	fun moveItem(fromPosition: Int, toPosition: Int) {
		val currentList = currentList.toMutableList()
		val item = currentList.removeAt(fromPosition)
		currentList.add(toPosition, item)
		submitList(currentList)
	}

	class ViewHolder(
		private val binding: ItemTranslationPreferenceBinding,
		private val clickListener: OnListItemClickListener<MangaTranslationPreference>
	) : RecyclerView.ViewHolder(binding.root) {

		@SuppressLint("ClickableViewAccessibility")
		fun bind(item: MangaTranslationPreference) {
			binding.root.setOnClickListener { 
				clickListener.onItemClick(item, it) 
			}

			binding.textBranch.text = item.branch ?: itemView.context.getString(R.string.default_translation)
			binding.textChapterCount.text = itemView.context.getString(R.string.translation_chapters_count, item.chapterCount)
			
			// Show priority number (1-based for user display)
			binding.textPriority.text = (item.priority + 1).toString()
			
			// Show enabled/disabled state
			binding.checkboxEnabled.isChecked = item.isEnabled
			binding.checkboxEnabled.setOnCheckedChangeListener { _, isChecked ->
				clickListener.onItemClick(item.copy(isEnabled = isChecked), binding.root)
			}

			// Show last used indicator
			binding.textLastUsed.isVisible = item.lastUsed != null
			if (item.lastUsed != null) {
				binding.textLastUsed.text = itemView.context.getString(R.string.recently_used)
			}

			// Set up drag handle
			binding.imageReorder.setOnTouchListener { _, event ->
				if (event.action == MotionEvent.ACTION_DOWN) {
					(itemView.context as? TranslationSettingsActivity)?.onDragHandleTouched(this)
				}
				false
			}

			// Visual state based on enabled/disabled
			val alpha = if (item.isEnabled) 1.0f else 0.6f
			binding.textBranch.alpha = alpha
			binding.textChapterCount.alpha = alpha
			binding.textPriority.alpha = alpha
		}
	}

	private class DiffCallback : DiffUtil.ItemCallback<MangaTranslationPreference>() {
		override fun areItemsTheSame(
			oldItem: MangaTranslationPreference,
			newItem: MangaTranslationPreference
		): Boolean = oldItem.branch == newItem.branch

		override fun areContentsTheSame(
			oldItem: MangaTranslationPreference,
			newItem: MangaTranslationPreference
		): Boolean = oldItem == newItem
	}
}