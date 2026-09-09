package com.gasguard.gasguard.data.repository

import com.gasguard.gasguard.data.model.DeviceDto
import com.gasguard.gasguard.data.model.LiveDataDto
import com.gasguard.gasguard.domain.model.DeviceLiveData
import com.gasguard.gasguard.domain.model.GasDevice
import com.gasguard.gasguard.domain.model.SensorStatus
import com.gasguard.gasguard.domain.repository.DeviceRepository
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
                val deviceFlows = ids.map { observeDevice(userId, it) }
                combine(deviceFlows) { devices ->
                    devices.filterNotNull()
                }
            }
        }
    }

    override suspend fun getDeviceById(deviceId: String): GasDevice? {
        return try {
            val snapshot = database.getReference("devices").child(deviceId).get().await()
            val deviceDto = snapshot.getValue(DeviceDto::class.java) ?: return null
            val userId = deviceDto.ownerUserId ?: return deviceDto.toDomain(null)
            
            val liveSnapshot = database.getReference("users").child(userId).child("devices").child(deviceId).child("liveData").get().await()
            val liveDataDto = liveSnapshot.getValue(LiveDataDto::class.java)
            
            deviceDto.toDomain(liveDataDto?.toDomain())
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

            val snapshot = deviceRef.get().await()
            val existingDevice = snapshot.getValue(DeviceDto::class.java)

            if (existingDevice != null) {
                if (existingDevice.ownerUserId != null && existingDevice.ownerUserId != userId) {
                    return Result.failure(Exception("This device is already registered to another user."))
                }
                
                deviceRef.child("ownerUserId").setValue(userId).await()
                deviceRef.child("deviceName").setValue(deviceName).await()
                deviceRef.child("location").setValue(location).await()
            } else {
                val newDevice = DeviceDto(
                    deviceId = deviceId,
                    ownerUserId = userId,
                    deviceName = deviceName,
                    location = location,
                    threshold = 30.0,
                    createdAt = System.currentTimeMillis()
                )
                deviceRef.setValue(newDevice).await()
            }

            userDeviceRef.child("deviceId").setValue(deviceId).await()
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
            database.getReference("users").child(userId).child("devices").child(deviceId).removeValue().await()
            
            val deviceRef = database.getReference("devices").child(deviceId)
            val snapshot = deviceRef.get().await()
            val device = snapshot.getValue(DeviceDto::class.java)
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
            val device = snapshot.getValue(DeviceDto::class.java)
            
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

    override suspend fun updateLiveDeviceData(
        userId: String,
        deviceId: String,
        gasLevel: Double,
        batteryLevel: Int?,
        sensorStatus: SensorStatus,
        lastHeartbeat: Long
    ): Result<Unit> {
        return try {
            val liveDataRef = database.getReference("users").child(userId).child("devices").child(deviceId).child("liveData")

            val liveData = mutableMapOf<String, Any>(
                "deviceId" to deviceId,
                "gasLevel" to gasLevel,
                "sensorStatus" to sensorStatus.name,
                "lastHeartbeat" to lastHeartbeat,
                "lastUpdated" to System.currentTimeMillis()
            )
            // No gasRaw in the simulation params, but we could add it if needed.
            // For now, let's keep it as is.

            liveDataRef.updateChildren(liveData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateHeartbeat(userId: String, deviceId: String): Result<Unit> {
        return try {
            val liveDataRef = database.getReference("users").child(userId).child("devices").child(deviceId).child("liveData")

            val updates = mapOf(
                "lastHeartbeat" to System.currentTimeMillis(),
                "lastUpdated" to System.currentTimeMillis()
            )
            liveDataRef.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeDevice(userId: String, deviceId: String): Flow<GasDevice?> {
        val metadataFlow = callbackFlow {
            val ref = database.getReference("devices").child(deviceId)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    trySend(snapshot.getValue(DeviceDto::class.java))
                }
                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }
            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }

        val liveDataFlow = observeDeviceLiveData(userId, deviceId)

        return combine(metadataFlow, liveDataFlow) { metadata, liveData ->
            metadata?.toDomain(liveData)
        }
    }

    override fun observeDeviceLiveData(userId: String, deviceId: String): Flow<DeviceLiveData?> {
        return callbackFlow {
            val ref = database.getReference("users").child(userId).child("devices").child(deviceId).child("liveData")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    trySend(snapshot.getValue(LiveDataDto::class.java)?.toDomain())
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
