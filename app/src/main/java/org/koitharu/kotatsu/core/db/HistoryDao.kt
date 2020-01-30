package org.koitharu.kotatsu.core.db

import androidx.room.Dao
import androidx.room.Query
import org.koitharu.kotatsu.core.db.entity.HistoryEntity

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history")
    suspend fun getAll(): List<HistoryEntity>
}