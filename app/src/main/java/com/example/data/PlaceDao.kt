package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Transaction
    @Query("SELECT * FROM places")
    fun getAllPlacesWithDetails(): Flow<List<PlaceWithDetails>>

    @Transaction
    @Query("SELECT * FROM places WHERE uid = :uid")
    fun getPlaceWithDetailsByUid(uid: String): Flow<PlaceWithDetails?>

    @Transaction
    @Query("SELECT * FROM places WHERE major = :major AND minor = :minor LIMIT 1")
    suspend fun getPlaceByMajorMinor(major: Int, minor: Int): PlaceWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaces(places: List<PlaceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShowcases(showcases: List<ShowcaseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<GameEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNavigation(navigations: List<NavigationEntity>)

    @Query("DELETE FROM places")
    suspend fun deleteAllPlaces()

    @Query("UPDATE places SET isVisited = 1 WHERE uid = :uid")
    suspend fun markPlaceAsVisited(uid: String)
}
