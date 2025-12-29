package com.pashaoleynik97.droiddeploy.core.dto.apikey

import com.pashaoleynik97.droiddeploy.core.domain.ApiKey
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class ApiKeyDto(
    @Schema(description = "API key unique identifier", example = "a1b2c3d4-e5f6-4789-a012-3b456c789def")
    val id: UUID,
    @Schema(description = "Human-readable API key name", example = "CI Pipeline Key")
    val name: String,
    @Schema(description = "API key role", example = "CI", allowableValues = ["CI", "CONSUMER"])
    val role: String,
    @Schema(description = "Application UUID this API key belongs to", example = "123e4567-e89b-12d3-a456-426614174000")
    val applicationId: UUID,
    @Schema(description = "Whether the API key is active (not revoked)", example = "true")
    val isActive: Boolean,
    @Schema(description = "Timestamp when the API key was created", example = "2025-12-29T10:30:00Z")
    val createdAt: Instant,
    @Schema(description = "Timestamp when the API key was last used for authentication", example = "2025-12-29T14:20:00Z", nullable = true)
    val lastUsedAt: Instant?,
    @Schema(description = "Timestamp when the API key expires (null if no expiration)", example = "2026-12-29T10:30:00Z", nullable = true)
    val expiresAt: Instant?,
    @Schema(description = "The actual API key value (only returned on creation, null otherwise)", example = "dd_ak_1a2b3c4d5e6f7g8h9i0j", nullable = true)
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
