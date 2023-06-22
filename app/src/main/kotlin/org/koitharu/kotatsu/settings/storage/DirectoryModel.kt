package org.koitharu.kotatsu.settings.storage

import androidx.annotation.StringRes
import org.koitharu.kotatsu.list.ui.model.ListModel
import java.io.File

class DirectoryModel(
	val title: String?,
	@StringRes val titleRes: Int,
	val file: File?,
	val isChecked: Boolean,
	val isAvailable: Boolean,
) : ListModel {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as DirectoryModel

		if (title != other.title) return false
		if (titleRes != other.titleRes) return false
		if (file != other.file) return false
		if (isChecked != other.isChecked) return false
		return isAvailable == other.isAvailable
	}

	override fun hashCode(): Int {
		var result = title?.hashCode() ?: 0
		result = 31 * result + titleRes
		result = 31 * result + (file?.hashCode() ?: 0)
		result = 31 * result + isChecked.hashCode()
		result = 31 * result + isAvailable.hashCode()
		return result
	}
}
