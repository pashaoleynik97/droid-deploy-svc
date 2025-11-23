package com.pashaoleynik97.droiddeploy.core.domain

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val login: String,
    val passwordHash: String?,
    val role: UserRole,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastLoginAt: Instant?,
    val lastInteractionAt: Instant?,
    val tokenVersion: Int
)
