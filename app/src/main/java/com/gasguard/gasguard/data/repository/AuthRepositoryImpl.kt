package com.gasguard.gasguard.data.repository

import com.gasguard.gasguard.domain.model.User
import com.gasguard.gasguard.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val database: FirebaseDatabase
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User not found")
            
            // Fetch additional user data from database if needed, 
            // but for now, we'll construct from FirebaseUser metadata
            val user = User(
                userId = firebaseUser.uid,
                name = firebaseUser.displayName ?: "",
                email = firebaseUser.email ?: "",
                profileImageUrl = firebaseUser.photoUrl?.toString(),
                createdAt = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
            )
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(name: String, email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Registration failed")
            
            val user = User(
                userId = firebaseUser.uid,
                name = name,
                email = email,
                profileImageUrl = null,
                createdAt = System.currentTimeMillis()
            )
            
            // Store user profile in Realtime Database
            database.getReference("users").child(user.userId).setValue(user).await()
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    override fun getCurrentUser(): Flow<User?> {
        return callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                val firebaseUser = auth.currentUser
                val user = firebaseUser?.let {
                    User(
                        userId = it.uid,
                        name = it.displayName ?: "",
                        email = it.email ?: "",
                        profileImageUrl = it.photoUrl?.toString(),
                        createdAt = it.metadata?.creationTimestamp ?: 0L
                    )
                }
                trySend(user)
            }
            firebaseAuth.addAuthStateListener(listener)
            awaitClose {
                firebaseAuth.removeAuthStateListener(listener)
            }
        }
    }
}
