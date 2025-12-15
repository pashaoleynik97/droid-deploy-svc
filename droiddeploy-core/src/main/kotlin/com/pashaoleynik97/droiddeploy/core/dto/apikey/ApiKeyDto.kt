package com.pashaoleynik97.droiddeploy.core.dto.apikey

import com.pashaoleynik97.droiddeploy.core.domain.ApiKey
import java.time.Instant
import java.util.UUID

data class ApiKeyDto(
    val id: UUID,
    val name: String,
    val role: String,
    val applicationId: UUID,
    val isActive: Boolean,
    val createdAt: Instant,
    val lastUsedAt: Instant?,
    val expiresAt: Instant?,
    val apiKey: String? = null
) {
    companion object {
        fun fromDomain(apiKey: ApiKey, rawApiKey: String? = null): ApiKeyDto {
            return ApiKeyDto(
                id = apiKey.id,
                name = apiKey.name,
                role = apiKey.role.name,
                applicationId = apiKey.applicationId,
                isActive = apiKey.isActive,
                createdAt = Instant.ofEpochMilli(apiKey.createdAt),
                lastUsedAt = apiKey.lastUsedAt?.let { Instant.ofEpochMilli(it) },
                expiresAt = apiKey.expiresAt?.let { Instant.ofEpochMilli(it) },
                apiKey = rawApiKey
            )
        }
    }
}
