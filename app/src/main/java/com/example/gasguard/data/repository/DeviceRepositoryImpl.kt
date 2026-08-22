package com.example.gasguard.data.repository

import com.example.gasguard.domain.model.DeviceStatus
import com.example.gasguard.domain.model.GasDevice
import com.example.gasguard.domain.repository.DeviceRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DeviceRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase
) : DeviceRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getUserDevices(userId: String): Flow<List<GasDevice>> {
        val deviceIdsFlow = callbackFlow {
            val ref = database.getReference("users").child(userId).child("devices")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val ids = snapshot.children.mapNotNull { it.key }
                    trySend(ids)
                }
                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }
            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }

        return deviceIdsFlow.flatMapLatest { ids ->
            if (ids.isEmpty()) {
                flowOf(emptyList())
            } else {
                val deviceFlows = ids.map { observeDevice(it) }
                combine(deviceFlows) { devices ->
                    devices.filterNotNull()
                }
            }
        }
    }

    override suspend fun getDeviceById(deviceId: String): GasDevice? {
        return try {
            val snapshot = database.getReference("devices").child(deviceId).get().await()
            snapshot.getValue(GasDevice::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun addDevice(
        userId: String,
        deviceId: String,
        deviceName: String,
        location: String
    ): Result<Unit> {
        return try {
            val deviceRef = database.getReference("devices").child(deviceId)
            val userDeviceRef = database.getReference("users").child(userId).child("devices").child(deviceId)

            // Check if device already exists and has an owner
            val snapshot = deviceRef.get().await()
            val existingDevice = snapshot.getValue(GasDevice::class.java)

            if (existingDevice != null) {
                if (existingDevice.ownerUserId != null && existingDevice.ownerUserId != userId) {
                    return Result.failure(Exception("This device is already registered to another user."))
                }
                
                // Claim/Re-claim device
                deviceRef.child("ownerUserId").setValue(userId).await()
                // Update metadata if needed
                deviceRef.child("deviceName").setValue(deviceName).await()
                deviceRef.child("location").setValue(location).await()
            } else {
                // Create new device
                val newDevice = GasDevice(
                    deviceId = deviceId,
                    ownerUserId = userId,
                    deviceName = deviceName,
                    location = location,
                    currentGasLevel = 0.0f,
                    threshold = 30.0,
                    status = DeviceStatus.SAFE,
                    isOnline = false,
                    createdAt = System.currentTimeMillis(),
                    lastUpdated = System.currentTimeMillis()
                )
                deviceRef.setValue(newDevice).await()
            }

            // Add reference to user
            userDeviceRef.setValue(true).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDeviceMetadata(
        deviceId: String,
        deviceName: String,
        location: String
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "deviceName" to deviceName,
                "location" to location,
                "lastUpdated" to System.currentTimeMillis()
            )
            database.getReference("devices").child(deviceId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteDeviceFromUser(userId: String, deviceId: String): Result<Unit> {
        return try {
            // Remove from user's list
            database.getReference("users").child(userId).child("devices").child(deviceId).removeValue().await()
            
            // Check if user was owner and release ownership
            val deviceRef = database.getReference("devices").child(deviceId)
            val snapshot = deviceRef.get().await()
            val device = snapshot.getValue(GasDevice::class.java)
            if (device?.ownerUserId == userId) {
                deviceRef.child("ownerUserId").removeValue().await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateThreshold(
        userId: String,
        deviceId: String,
        threshold: Double
    ): Result<Unit> {
        return try {
            val deviceRef = database.getReference("devices").child(deviceId)
            val snapshot = deviceRef.get().await()
            val device = snapshot.getValue(GasDevice::class.java)
            
            if (device?.ownerUserId != userId) {
                return Result.failure(Exception("You do not have permission to update this device."))
            }
            
            val updates = mapOf(
                "threshold" to threshold,
                "lastUpdated" to System.currentTimeMillis()
            )
            deviceRef.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeDevice(deviceId: String): Flow<GasDevice?> {
        return callbackFlow {
            val ref = database.getReference("devices").child(deviceId)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val device = snapshot.getValue(GasDevice::class.java)
                    trySend(device)
                }
                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }
            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }
    }
}
