package com.pashaoleynik97.droiddeploy.core.domain

import java.util.UUID

data class ApiKey(
    val id: UUID,
    val name: String,
    val valueHash: String,
    val role: ApiKeyRole,
    val applicationId: UUID,
    val isActive: Boolean,
    val createdAt: Long,
    val lastUsedAt: Long?,
    val expiresAt: Long?,
    val tokenVersion: Int = 0
)
