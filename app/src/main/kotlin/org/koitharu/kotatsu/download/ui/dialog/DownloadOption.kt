package org.koitharu.kotatsu.download.ui.dialog

import android.content.res.Resources
import androidx.annotation.DrawableRes
import org.koitharu.kotatsu.R
import java.util.Locale
import com.google.android.material.R as materialR

sealed interface DownloadOption {

	val chaptersIds: Set<Long>

	@get:DrawableRes
	val iconResId: Int

	val chaptersCount: Int
		get() = chaptersIds.size

	fun getLabel(resources: Resources): CharSequence

	class AllChapters(
		val branch: String,
		override val chaptersIds: Set<Long>,
	) : DownloadOption {

		override val iconResId = R.drawable.ic_select_group

		override fun getLabel(resources: Resources): CharSequence {
			return resources.getString(R.string.download_option_all_chapters, branch)
		}
	}

	class WholeManga(
		override val chaptersIds: Set<Long>,
	) : DownloadOption {

		override val iconResId = materialR.drawable.abc_ic_menu_selectall_mtrl_alpha

		override fun getLabel(resources: Resources): CharSequence {
			return resources.getString(R.string.download_option_whole_manga)
		}
	}

	class FirstChapters(
		override val chaptersIds: Set<Long>,
	) : DownloadOption {

		override val iconResId = R.drawable.ic_list_start

		override fun getLabel(resources: Resources): CharSequence {
			return resources.getString(
				R.string.download_option_first_n_chapters,
				resources.getQuantityString(R.plurals.chapters, chaptersCount, chaptersCount)
					.lowercase(Locale.getDefault()),
			)
		}
	}

	class AllUnreadChapters(
		override val chaptersIds: Set<Long>,
		val branch: String?,
	) : DownloadOption {

		override val iconResId = R.drawable.ic_list_end

		override fun getLabel(resources: Resources): CharSequence {
			return if (branch == null) {
				resources.getString(R.string.download_option_all_unread)
			} else {
				resources.getString(R.string.download_option_all_unread_b, branch)
			}
		}
	}

	class NextUnreadChapters(
		override val chaptersIds: Set<Long>,
	) : DownloadOption {

		override val iconResId = R.drawable.ic_list_next

		override fun getLabel(resources: Resources): CharSequence {
			return resources.getString(
				R.string.download_option_next_unread_n_chapters,
				resources.getQuantityString(R.plurals.chapters, chaptersCount, chaptersCount)
					.lowercase(Locale.getDefault()),
			)
		}
	}

	class SelectionHint : DownloadOption {

		override val chaptersIds: Set<Long> = emptySet()
		override val iconResId = R.drawable.ic_tap

		override fun getLabel(resources: Resources): CharSequence {
			return resources.getString(R.string.download_option_manual_selection)
		}
	}
}
