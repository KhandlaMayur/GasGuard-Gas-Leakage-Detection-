package com.gasguard.gasguard.data.repository

import com.gasguard.gasguard.domain.model.AlertStatus
import com.gasguard.gasguard.domain.model.GasAlert
import com.gasguard.gasguard.domain.repository.AlertRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AlertRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase
) : AlertRepository {

    private fun getAlertsRef(userId: String) = 
        database.getReference("users").child(userId).child("alerts")

    override fun observeActiveAlerts(userId: String): Flow<List<GasAlert>> {
        return observeAllAlerts(userId).map { alerts ->
            alerts.filter { it.status == AlertStatus.ACTIVE }
                .sortedByDescending { it.createdAt }
        }
    }

    override fun observeAlertHistory(userId: String): Flow<List<GasAlert>> {
        return observeAllAlerts(userId).map { alerts ->
            val historyStatuses = listOf(AlertStatus.ACKNOWLEDGED, AlertStatus.EXPIRED, AlertStatus.RESOLVED)
            alerts.filter { it.status in historyStatuses }
                .sortedByDescending { it.acknowledgedAt ?: it.expiredAt ?: it.createdAt }
        }
    }

    private fun observeAllAlerts(userId: String): Flow<List<GasAlert>> = callbackFlow {
        val ref = getAlertsRef(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val alerts = snapshot.children.mapNotNull { it.getValue(GasAlert::class.java) }
                trySend(alerts)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeAlertById(userId: String, alertId: String): Flow<GasAlert?> = callbackFlow {
        val ref = getAlertsRef(userId).child(alertId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(GasAlert::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun createAlertIfNeeded(alert: GasAlert): Result<Unit> {
        return try {
            val ref = getAlertsRef(alert.ownerUserId)
            
            // Check for existing ACTIVE alert for this device
            val existingActiveAlert = getActiveAlertForDevice(alert.ownerUserId, alert.deviceId)
            
            if (existingActiveAlert != null) {
                // Duplicate prevention and escalation
                if (alert.severity != existingActiveAlert.severity || alert.gasLevel != existingActiveAlert.gasLevel) {
                    val updates = mapOf(
                        "severity" to alert.severity,
                        "gasLevel" to alert.gasLevel,
                        "threshold" to alert.threshold
                    )
                    ref.child(existingActiveAlert.alertId).updateChildren(updates).await()
                }
                Result.success(Unit)
            } else {
                // Create new alert
                val newAlertRef = ref.push()
                val finalAlert = alert.copy(alertId = newAlertRef.key ?: "")
                newAlertRef.setValue(finalAlert).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acknowledgeAlert(userId: String, alertId: String): Result<Unit> {
        return try {
            val ref = getAlertsRef(userId).child(alertId)
            val snapshot = ref.get().await()
            val alert = snapshot.getValue(GasAlert::class.java)
            
            if (alert != null && alert.status == AlertStatus.ACTIVE) {
                val currentTime = System.currentTimeMillis()
                if (currentTime < alert.acknowledgementDeadline) {
                    val updates = mapOf(
                        "status" to AlertStatus.ACKNOWLEDGED,
                        "acknowledgedAt" to currentTime
                    )
                    ref.updateChildren(updates).await()
                    Result.success(Unit)
                } else {
                    expireAlert(userId, alertId)
                    Result.failure(Exception("Acknowledgement time expired"))
                }
            } else {
                Result.failure(Exception("Alert is not active"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun expireAlert(userId: String, alertId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to AlertStatus.EXPIRED,
                "expiredAt" to System.currentTimeMillis()
            )
            getAlertsRef(userId).child(alertId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActiveAlertForDevice(userId: String, deviceId: String): GasAlert? {
        return try {
            val snapshot = getAlertsRef(userId)
                .orderByChild("deviceId")
                .equalTo(deviceId)
                .get()
                .await()
            
            snapshot.children.mapNotNull { it.getValue(GasAlert::class.java) }
                .find { it.status == AlertStatus.ACTIVE }
        } catch (e: Exception) {
            null
        }
    }
}
