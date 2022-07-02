package org.koitharu.kotatsu.favourites.ui.categories

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory
import com.google.android.material.R as materialR

class CategoriesEditDelegate(
	private val context: Context,
	private val callback: CategoriesEditCallback
) {

	fun deleteCategory(category: FavouriteCategory) {
		MaterialAlertDialogBuilder(context, materialR.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
			.setMessage(context.getString(R.string.category_delete_confirm, category.title))
			.setTitle(R.string.remove_category)
			.setIcon(R.drawable.ic_delete)
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