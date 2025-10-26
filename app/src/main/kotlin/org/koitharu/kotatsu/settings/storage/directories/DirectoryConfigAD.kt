package org.koitharu.kotatsu.settings.storage.directories

import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isGone
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.FileSize
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.setTooltipCompat
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ItemStorageConfig2Binding

fun directoryConfigAD(
    clickListener: OnListItemClickListener<DirectoryConfigModel>,
) = adapterDelegateViewBinding<DirectoryConfigModel, DirectoryConfigModel, ItemStorageConfig2Binding>(
    { layoutInflater, parent -> ItemStorageConfig2Binding.inflate(layoutInflater, parent, false) },
) {

    binding.buttonRemove.setOnClickListener { v -> clickListener.onItemClick(item, v) }
    binding.buttonRemove.setTooltipCompat(binding.buttonRemove.contentDescription)

    bind {
        binding.textViewTitle.text = item.title
        binding.textViewSubtitle.text = item.path.absolutePath
        binding.buttonRemove.isGone = item.isAppPrivate
        binding.buttonRemove.isEnabled = !item.isDefault
        binding.spacer.visibility = if (item.isAppPrivate) {
            View.INVISIBLE
        } else {
            View.GONE
        }
        binding.textViewInfo.textAndVisible = buildSpannedString {
            if (item.isDefault) {
                bold {
                    append(getString(R.string.download_default_directory))
                }
            }
            if (!item.isAccessible) {
                if (isNotEmpty()) appendLine()
                color(
                    context.getThemeColor(
                        androidx.appcompat.R.attr.colorError,
                        ContextCompat.getColor(context, R.color.common_red),
                    ),
                ) {
                    append(getString(R.string.no_write_permission_to_file))
                }
            }
            if (item.isAppPrivate) {
                if (isNotEmpty()) appendLine()
                append(getString(R.string.private_app_directory_warning))
            }
        }
        binding.indicatorSize.max = FileSize.BYTES.convert(item.available, FileSize.KILOBYTES).toInt()
        binding.indicatorSize.progress = FileSize.BYTES.convert(item.size, FileSize.KILOBYTES).toInt()
        binding.textViewSize.text = context.getString(
            R.string.available_pattern,
            FileSize.BYTES.format(context, item.available),
        )
    }
}
