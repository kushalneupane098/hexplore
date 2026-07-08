package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ==========================================
// ROOM ENTITIES
// ==========================================

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val uid: String,
    val locationName: String,
    val shortDescription: String,
    val imageAsset: String,
    val major: Int,
    val minor: Int,
    val about: String,
    val imageUrl: String? = null,
    val isVisited: Boolean = false
)

@Entity(
    tableName = "showcases",
    foreignKeys = [
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["uid"],
            childColumns = ["placeUid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["placeUid"])]
)
data class ShowcaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val placeUid: String,
    val title: String,
    val desc: String,
    val imageUrl: String? = null,
    val category: String? = null
)

@Entity(
    tableName = "games",
    foreignKeys = [
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["uid"],
            childColumns = ["placeUid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["placeUid"])]
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val placeUid: String,
    val title: String,
    val desc: String,
    val imageUrl: String? = null
)

@Entity(
    tableName = "navigations",
    foreignKeys = [
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["uid"],
            childColumns = ["placeUid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["placeUid"])]
)
data class NavigationEntity(
    @PrimaryKey val placeUid: String,
    val inFrontUid: String?,
    val inFrontName: String?,
    val behindUid: String?,
    val behindName: String?,
    val leftUid: String?,
    val leftName: String?,
    val rightUid: String?,
    val rightName: String?
)

// ==========================================
// ROOM RELATION (REPRESENTING FULL LEVEL 3 DEPTH)
// ==========================================

data class PlaceWithDetails(
    @Embedded val place: PlaceEntity,
    
    @Relation(
        parentColumn = "uid",
        entityColumn = "placeUid"
    )
    val showcases: List<ShowcaseEntity>,
    
    @Relation(
        parentColumn = "uid",
        entityColumn = "placeUid"
    )
    val games: List<GameEntity>,
    
    @Relation(
        parentColumn = "uid",
        entityColumn = "placeUid"
    )
    val navigation: NavigationEntity?
)

// ==========================================
// MOSHI JSON MODELS (FOR PARSING hex_data.json)
// ==========================================

@JsonClass(generateAdapter = true)
data class JsonBeacons(
    val beacons: Map<String, JsonBeacon>
)

@JsonClass(generateAdapter = true)
data class JsonBeacon(
    @Json(name = "location_name") val locationName: String,
    @Json(name = "short_description") val shortDescription: String,
    @Json(name = "image_asset") val imageAsset: String,
    val major: Int,
    val minor: Int,
    @Json(name = "detailed_info") val detailedInfo: JsonDetailedInfo,
    val navigation: JsonNavigation,
    @Json(name = "image_url") val imageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class JsonDetailedInfo(
    val about: String,
    val showcases: List<JsonShowcase>,
    val games: List<JsonGame>
)

@JsonClass(generateAdapter = true)
data class JsonShowcase(
    val title: String,
    val desc: String,
    @Json(name = "image_url") val imageUrl: String? = null,
    val category: String? = null
)

@JsonClass(generateAdapter = true)
data class JsonGame(
    val title: String,
    val desc: String,
    @Json(name = "image_url") val imageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class JsonNavigation(
    @Json(name = "in_front") val inFront: JsonNavigationTarget?,
    val behind: JsonNavigationTarget?,
    val left: JsonNavigationTarget?,
    val right: JsonNavigationTarget?
)

@JsonClass(generateAdapter = true)
data class JsonNavigationTarget(
    @Json(name = "target_uid") val targetUid: String,
    val name: String
)
