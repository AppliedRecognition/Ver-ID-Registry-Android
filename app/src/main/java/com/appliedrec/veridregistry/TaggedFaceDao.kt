package com.appliedrec.veridregistry

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaggedFaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(taggedFace: TaggedFaceEntity): Long

    @Query("SELECT * FROM tagged_faces ORDER BY date_added DESC")
    fun getAll(): Flow<List<TaggedFaceEntity>>

    @Query("SELECT * FROM tagged_faces WHERE user_name = :userName ORDER BY date_added DESC")
    fun getUserFaces(userName: String): Flow<List<TaggedFaceEntity>>

    @Query("SELECT DISTINCT user_name FROM tagged_faces ORDER BY user_name")
    fun getUsers(): Flow<List<String>>

    @Query("SELECT COUNT(DISTINCT user_name) FROM tagged_faces")
    fun getUserCount(): Flow<Int>

    @Query("""
        SELECT tf.*
        FROM tagged_faces tf
        INNER JOIN (
            SELECT user_name, MAX(date_added) AS max_date, MAX(id) AS max_id
            FROM tagged_faces
            GROUP BY user_name
        ) grouped
        ON tf.user_name = grouped.user_name AND tf.date_added = grouped.max_date AND tf.id = grouped.max_id
        ORDER BY tf.user_name ASC
    """)
    fun getLatestUserFaces(): Flow<List<TaggedFaceEntity>>

    @Delete
    suspend fun delete(taggedFace: TaggedFaceEntity)

    @Query("DELETE FROM tagged_faces WHERE id IN (:ids)")
    suspend fun delete(vararg ids: Long)

    @Query("DELETE FROM tagged_faces")
    suspend fun deleteAll()

    @Query("DELETE FROM tagged_faces WHERE user_name = :userName")
    suspend fun deleteUser(userName: String)
}
