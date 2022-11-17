package org.koitharu.kotatsu.shelf.ui.config

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.CompoundButton
import androidx.core.view.updatePaddingRelative
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.ItemCategoryCheckableMultipleBinding
import org.koitharu.kotatsu.databinding.ItemShelfSectionDraggableBinding
import org.koitharu.kotatsu.shelf.domain.ShelfSection

@SuppressLint("ClickableViewAccessibility")
fun shelfSectionAD(
	listener: ShelfSettingsListener,
) =
	adapterDelegateViewBinding<ShelfSettingsItemModel.Section, ShelfSettingsItemModel, ItemShelfSectionDraggableBinding>(
		{ layoutInflater, parent -> ItemShelfSectionDraggableBinding.inflate(layoutInflater, parent, false) },
	) {

		val eventListener = object :
			View.OnTouchListener,
			CompoundButton.OnCheckedChangeListener {

			override fun onTouch(v: View?, event: MotionEvent): Boolean {
				return if (event.actionMasked == MotionEvent.ACTION_DOWN) {
					listener.onDragHandleTouch(this@adapterDelegateViewBinding)
					true
				} else {
					false
				}
			}

			override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
				listener.onItemCheckedChanged(item, isChecked)
			}
		}

		binding.switchToggle.setOnCheckedChangeListener(eventListener)
		binding.imageViewHandle.setOnTouchListener(eventListener)

		bind {
			binding.textViewTitle.setText(item.section.titleResId)
			binding.switchToggle.isChecked = item.isChecked
		}
	}

fun shelfCategoryAD(
	listener: ShelfSettingsListener,
) =
	adapterDelegateViewBinding<ShelfSettingsItemModel.FavouriteCategory, ShelfSettingsItemModel, ItemCategoryCheckableMultipleBinding>(
		{ layoutInflater, parent -> ItemCategoryCheckableMultipleBinding.inflate(layoutInflater, parent, false) },
	) {
		itemView.setOnClickListener {
			listener.onItemCheckedChanged(item, !item.isChecked)
		}
		binding.root.updatePaddingRelative(
			start = binding.root.paddingStart * 2,
			end = binding.root.paddingStart,
		)

		bind {
			binding.root.text = item.title
			binding.root.isChecked = item.isChecked
		}
	}

private val ShelfSection.titleResId: Int
	get() = when (this) {
		ShelfSection.HISTORY -> R.string.history
		ShelfSection.LOCAL -> R.string.local_storage
		ShelfSection.UPDATED -> R.string.updated
		ShelfSection.FAVORITES -> R.string.favourites
	}
