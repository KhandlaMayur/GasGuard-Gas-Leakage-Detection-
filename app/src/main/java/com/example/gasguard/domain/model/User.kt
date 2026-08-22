package com.example.gasguard.domain.model

data class User(
    val userId: String,
    val name: String,
    val email: String,
    val profileImageUrl: String?,
    val createdAt: Long
)
