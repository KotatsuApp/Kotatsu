package org.koitharu.kotatsu.core.db

import androidx.room.*
import org.koitharu.kotatsu.core.db.entity.MangaPrefsEntity

@Dao
abstract class PreferencesDao {

	@Query("SELECT * FROM preferences WHERE manga_id = :mangaId")
	abstract suspend fun find(mangaId: Long): MangaPrefsEntity?

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	abstract suspend fun insert(pref: MangaPrefsEntity): Long

	@Update
	abstract suspend fun update(pref: MangaPrefsEntity): Int

	@Transaction
	open suspend fun upsert(pref: MangaPrefsEntity) {
		if (update(pref) == 0) {
			insert(pref)
		}
	}
}