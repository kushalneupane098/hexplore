package com.example.data

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class PlaceRepository(
    private val context: Context,
    private val placeDao: PlaceDao
) {
    private val TAG = "PlaceRepository"

    val allPlaces: Flow<List<PlaceWithDetails>> = placeDao.getAllPlacesWithDetails()

    fun getPlaceByUid(uid: String): Flow<PlaceWithDetails?> {
        return placeDao.getPlaceWithDetailsByUid(uid)
    }

    suspend fun getPlaceByMajorMinor(major: Int, minor: Int): PlaceWithDetails? {
        return withContext(Dispatchers.IO) {
            placeDao.getPlaceByMajorMinor(major, minor)
        }
    }

    suspend fun populateDatabaseIfEmpty() {
        withContext(Dispatchers.IO) {
            try {
                // Check if database already has places
                val existingPlaces = placeDao.getAllPlacesWithDetails().first()
                if (existingPlaces.isNotEmpty()) {
                    Log.d(TAG, "Database already populated with ${existingPlaces.size} places")
                    return@withContext
                }

                Log.d(TAG, "Database is empty. Starting prepopulation from assets...")

                // Read hex_data.json
                val jsonString = context.assets.open("hex_data.json").bufferedReader().use { it.readText() }

                // Parse with Moshi
                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()
                val adapter = moshi.adapter(JsonBeacons::class.java)
                val response = adapter.fromJson(jsonString)

                if (response != null) {
                    val placesToInsert = mutableListOf<PlaceEntity>()
                    val showcasesToInsert = mutableListOf<ShowcaseEntity>()
                    val gamesToInsert = mutableListOf<GameEntity>()
                    val navigationsToInsert = mutableListOf<NavigationEntity>()

                    response.beacons.forEach { (uid, beacon) ->
                        // 1. Create PlaceEntity
                        placesToInsert.add(
                            PlaceEntity(
                                uid = uid,
                                locationName = beacon.locationName,
                                shortDescription = beacon.shortDescription,
                                imageAsset = beacon.imageAsset,
                                major = beacon.major,
                                minor = beacon.minor,
                                about = beacon.detailedInfo.about,
                                imageUrl = beacon.imageUrl
                            )
                        )

                        // 2. Create ShowcaseEntities
                        beacon.detailedInfo.showcases.forEach { showcase ->
                            showcasesToInsert.add(
                                ShowcaseEntity(
                                    placeUid = uid,
                                    title = showcase.title,
                                    desc = showcase.desc,
                                    imageUrl = showcase.imageUrl,
                                    category = showcase.category
                                )
                            )
                        }

                        // 3. Create GameEntities
                        beacon.detailedInfo.games.forEach { game ->
                            gamesToInsert.add(
                                GameEntity(
                                    placeUid = uid,
                                    title = game.title,
                                    desc = game.desc,
                                    imageUrl = game.imageUrl
                                )
                            )
                        }

                        // 4. Create NavigationEntity
                        navigationsToInsert.add(
                            NavigationEntity(
                                placeUid = uid,
                                inFrontUid = beacon.navigation.inFront?.targetUid,
                                inFrontName = beacon.navigation.inFront?.name,
                                behindUid = beacon.navigation.behind?.targetUid,
                                behindName = beacon.navigation.behind?.name,
                                leftUid = beacon.navigation.left?.targetUid,
                                leftName = beacon.navigation.left?.name,
                                rightUid = beacon.navigation.right?.targetUid,
                                rightName = beacon.navigation.right?.name
                            )
                        )
                    }

                    // Perform insertions in order of parent-child relationships
                    placeDao.insertPlaces(placesToInsert)
                    placeDao.insertShowcases(showcasesToInsert)
                    placeDao.insertGames(gamesToInsert)
                    placeDao.insertNavigation(navigationsToInsert)

                    Log.d(TAG, "Successfully prepopulated database with ${placesToInsert.size} locations")
                } else {
                    Log.e(TAG, "Failed to parse hex_data.json: Parsed response is null")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error prepopulating database", e)
            }
        }
    }
}
