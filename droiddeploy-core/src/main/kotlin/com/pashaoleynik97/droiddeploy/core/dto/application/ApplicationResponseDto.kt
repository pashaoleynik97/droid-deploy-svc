package com.pashaoleynik97.droiddeploy.core.dto.application

import com.pashaoleynik97.droiddeploy.core.domain.Application
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class ApplicationResponseDto(
    @Schema(description = "Application unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    val id: UUID,
    @Schema(description = "Application display name", example = "My Android App")
    val name: String,
    @Schema(description = "Android application bundle ID", example = "com.example.myapp")
    val bundleId: String,
    @Schema(description = "Timestamp when the application was created", example = "2025-12-29T10:30:00Z")
    val createdAt: Instant
) {
    companion object {
        fun fromDomain(application: Application): ApplicationResponseDto {
            return ApplicationResponseDto(
                id = application.id,
                name = application.name,
                bundleId = application.bundleId,
                createdAt = Instant.ofEpochMilli(application.createdAt)
            )
        }
    }
}