package org.koitharu.kotatsu.core.image

import android.os.Parcel
import android.os.Parcelable
import android.view.View
import androidx.collection.ArrayMap
import coil3.memory.MemoryCache
import coil3.request.SuccessResult
import coil3.util.CoilUtils
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

@Parcelize
class CoilMemoryCacheKey(
	val data: MemoryCache.Key
) : Parcelable {

	companion object : Parceler<CoilMemoryCacheKey> {
		override fun CoilMemoryCacheKey.write(parcel: Parcel, flags: Int) = with(data) {
			parcel.writeString(key)
			parcel.writeInt(extras.size)
			for (entry in extras.entries) {
				parcel.writeString(entry.key)
				parcel.writeString(entry.value)
			}
		}

		override fun create(parcel: Parcel): CoilMemoryCacheKey = CoilMemoryCacheKey(
			MemoryCache.Key(
				key = parcel.readString().orEmpty(),
				extras = run {
					val size = parcel.readInt()
					val map = ArrayMap<String, String>(size)
					repeat(size) {
						map.put(parcel.readString(), parcel.readString())
					}
					map
				},
			),
		)

		fun from(view: View): CoilMemoryCacheKey? {
			return (CoilUtils.result(view) as? SuccessResult)?.memoryCacheKey?.let {
				CoilMemoryCacheKey(it)
			}
		}
	}
}
