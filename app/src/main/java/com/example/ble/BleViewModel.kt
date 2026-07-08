package com.example.ble

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PlaceRepository
import com.example.data.PlaceWithDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BleViewModel(private val repository: PlaceRepository) : ViewModel() {
    private val TAG = "BleViewModel"

    // Current active detected place (Level 2/3)
    private val _currentPlace = MutableStateFlow<PlaceWithDetails?>(null)
    val currentPlace: StateFlow<PlaceWithDetails?> = _currentPlace.asStateFlow()

    // Global tracking of completed event challenges/quests
    private val _completedQuests = MutableStateFlow<Set<String>>(emptySet())
    val completedQuests: StateFlow<Set<String>> = _completedQuests.asStateFlow()

    fun toggleQuestCompletion(questTitle: String) {
        val current = _completedQuests.value
        if (current.contains(questTitle)) {
            _completedQuests.value = current - questTitle
        } else {
            _completedQuests.value = current + questTitle
        }
    }

    // Tracking list of all places for tags
    val allPlaces: StateFlow<List<PlaceWithDetails>> = repository.allPlaces
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isScanning: StateFlow<Boolean> = BleSignalTracker.isScanning

    init {
        // Populate database if empty
        viewModelScope.launch {
            repository.populateDatabaseIfEmpty()
        }
    }

    fun selectPlaceManually(place: PlaceWithDetails) {
        _currentPlace.value = place
    }

    fun startScannerService(context: Context) {
        try {
            val intent = Intent(context, BleScannerService::class.java)
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting scanner service", e)
        }
    }

    fun stopScannerService(context: Context) {
        try {
            val intent = Intent(context, BleScannerService::class.java)
            context.stopService(intent)
            BleSignalTracker.clearBeacon()
            _currentPlace.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scanner service", e)
        }
    }

    fun simulateBeacon(uid: String) {
        viewModelScope.launch {
            Log.d(TAG, "Simulating beacon proximity: UID=$uid")
            repository.getPlaceByUid(uid).collect { place ->
                if (place != null) {
                    BleSignalTracker.updateBeacon(uid, -50)
                    _currentPlace.value = place
                }
            }
        }
    }

    fun clearCurrentPlace() {
        BleSignalTracker.clearBeacon()
        _currentPlace.value = null
    }

    fun navigateToPlace(placeUid: String) {
        viewModelScope.launch {
            // Find the place with this UID from the database list
            val places = repository.allPlaces.first()
            val target = places.find { it.place.uid == placeUid }
            if (target != null) {
                _currentPlace.value = target
            }
        }
    }
}

class BleViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BleViewModel::class.java)) {
            val db = AppDatabase.getDatabase(context)
            val repository = PlaceRepository(context, db.placeDao())
            @Suppress("UNCHECKED_CAST")
            return BleViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
