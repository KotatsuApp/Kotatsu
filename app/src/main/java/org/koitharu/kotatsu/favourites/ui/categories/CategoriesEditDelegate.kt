package org.koitharu.kotatsu.favourites.ui.categories

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory

class CategoriesEditDelegate(
	private val context: Context,
	private val callback: CategoriesEditCallback
) {

	fun deleteCategory(category: FavouriteCategory) {
		MaterialAlertDialogBuilder(context)
			.setMessage(context.getString(R.string.category_delete_confirm, category.title))
			.setTitle(R.string.remove_category)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.remove) { _, _ ->
				callback.onDeleteCategory(category)
			}.create()
			.show()
	}

	interface CategoriesEditCallback {

		fun onDeleteCategory(category: FavouriteCategory)
	}
}