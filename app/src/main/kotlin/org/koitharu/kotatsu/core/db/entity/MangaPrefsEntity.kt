package org.koitharu.kotatsu.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
	tableName = "preferences",
	foreignKeys = [
		ForeignKey(
			entity = MangaEntity::class,
			parentColumns = ["manga_id"],
			childColumns = ["manga_id"],
			onDelete = ForeignKey.CASCADE,
		),
	],
)
data class MangaPrefsEntity(
	@PrimaryKey(autoGenerate = false)
	@ColumnInfo(name = "manga_id")
	val mangaId: Long,
	@ColumnInfo(name = "mode") val mode: Int,
	@ColumnInfo(name = "cf_brightness") val cfBrightness: Float,
	@ColumnInfo(name = "cf_contrast") val cfContrast: Float,
	@ColumnInfo(name = "cf_invert") val cfInvert: Boolean,
	@ColumnInfo(name = "cf_grayscale") val cfGrayscale: Boolean,
)
