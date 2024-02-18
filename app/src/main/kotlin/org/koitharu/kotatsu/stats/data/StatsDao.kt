package org.koitharu.kotatsu.stats.data

import androidx.room.Dao
import androidx.room.Upsert

@Dao
abstract class StatsDao {

	@Upsert
	abstract suspend fun upsert(entity: StatsEntity)
}
