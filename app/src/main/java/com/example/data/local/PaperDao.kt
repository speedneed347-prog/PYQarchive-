package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperDao {

    @Query("SELECT * FROM papers WHERE id = :id LIMIT 1")
    fun getPaperFlow(id: String): Flow<PaperEntity?>

    @Query("SELECT * FROM papers WHERE id = :id LIMIT 1")
    suspend fun getPaperDirect(id: String): PaperEntity?

    @Query("SELECT * FROM papers WHERE status = 'published' ORDER BY viewCount DESC LIMIT :limit")
    fun getPopularPapers(limit: Int = 10): Flow<List<PaperEntity>>

    @Query("SELECT * FROM papers WHERE lastViewedTimestamp IS NOT NULL ORDER BY lastViewedTimestamp DESC LIMIT :limit")
    fun getRecentPapers(limit: Int = 10): Flow<List<PaperEntity>>

    @Query("SELECT * FROM papers WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoritePapers(): Flow<List<PaperEntity>>

    @Query("SELECT * FROM papers WHERE downloadedFilePath IS NOT NULL ORDER BY downloadedAt DESC")
    fun getDownloadedPapers(): Flow<List<PaperEntity>>

    @Query("SELECT * FROM papers WHERE universityId = :uniId AND status = 'published' ORDER BY year DESC, title ASC")
    fun getPapersByUniversity(uniId: String): Flow<List<PaperEntity>>

    @Query("SELECT * FROM papers WHERE status = 'published'")
    fun getAllPapers(): Flow<List<PaperEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaper(paper: PaperEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPapers(papers: List<PaperEntity>)

    @Query("UPDATE papers SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: String, isFav: Boolean)

    @Query("UPDATE papers SET personalNotes = :notes WHERE id = :id")
    suspend fun updateNotes(id: String, notes: String)

    @Query("UPDATE papers SET lastViewedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateViewTimestamp(id: String, timestamp: Long)

    @Query("UPDATE papers SET downloadedFilePath = :path, downloadedSizeBytes = :size, downloadedAt = :timestamp WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, path: String?, size: Long, timestamp: Long?)

    @Query("UPDATE papers SET downloadedFilePath = NULL, downloadedSizeBytes = 0, downloadedAt = NULL WHERE id = :id")
    suspend fun clearDownload(id: String)

    @Query("UPDATE papers SET downloadedFilePath = NULL, downloadedSizeBytes = 0, downloadedAt = NULL")
    suspend fun clearAllDownloads()

    @Transaction
    suspend fun upsertPreservingLocalState(newPapers: List<PaperEntity>) {
        newPapers.forEach { newP ->
            val existing = getPaperDirect(newP.id)
            if (existing != null) {
                val merged = newP.copy(
                    isFavorite = existing.isFavorite,
                    personalNotes = existing.personalNotes,
                    lastViewedTimestamp = existing.lastViewedTimestamp,
                    downloadedFilePath = existing.downloadedFilePath,
                    downloadedSizeBytes = existing.downloadedSizeBytes,
                    downloadedAt = existing.downloadedAt
                )
                insertPaper(merged)
            } else {
                insertPaper(newP)
            }
        }
    }
}
