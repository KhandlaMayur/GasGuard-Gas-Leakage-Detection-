package com.gasguard.gasguard.data.repository

import com.gasguard.gasguard.domain.model.GasReading
import com.gasguard.gasguard.domain.repository.ReadingRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ReadingRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase
) : ReadingRepository {

    private fun getReadingsRef(userId: String, deviceId: String) =
        database.getReference("users").child(userId).child("readings").child(deviceId)

    override suspend fun recordReading(userId: String, reading: GasReading): Result<Unit> {
        return try {
            val ref = getReadingsRef(userId, reading.deviceId).push()
            val finalReading = reading.copy(readingId = ref.key ?: "")
            ref.setValue(finalReading).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeReadings(userId: String, deviceId: String): Flow<List<GasReading>> = callbackFlow {
        val ref = getReadingsRef(userId, deviceId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val readings = snapshot.children.mapNotNull { it.getValue(GasReading::class.java) }
                trySend(readings.sortedByDescending { it.timestamp })
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun getLatestReading(userId: String, deviceId: String): GasReading? {
        return try {
            val snapshot = getReadingsRef(userId, deviceId)
                .orderByChild("timestamp")
                .limitToLast(1)
                .get()
                .await()
            snapshot.children.firstOrNull()?.getValue(GasReading::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
