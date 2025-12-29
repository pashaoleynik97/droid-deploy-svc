package com.pashaoleynik97.droiddeploy.core.dto.user

import com.pashaoleynik97.droiddeploy.core.domain.User
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class UserResponseDto(
    @Schema(description = "User unique identifier", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    val id: UUID,
    @Schema(description = "User login (username)", example = "admin")
    val login: String,
    @Schema(description = "User role", example = "ADMIN", allowableValues = ["ADMIN", "CI", "CONSUMER"])
    val role: String,
    @Schema(description = "Whether the user account is active", example = "true")
    val isActive: Boolean,
    @Schema(description = "Timestamp when the user was created", example = "2025-12-29T10:30:00Z")
    val createdAt: Instant,
    @Schema(description = "Timestamp when the user was last updated", example = "2025-12-29T10:30:00Z")
    val updatedAt: Instant,
    @Schema(description = "Timestamp of last successful login", example = "2025-12-29T14:20:00Z", nullable = true)
    val lastLoginAt: Instant?,
    @Schema(description = "Timestamp of last API interaction", example = "2025-12-29T14:25:00Z", nullable = true)
    val lastInteractionAt: Instant?
) {
    companion object {
        fun fromDomain(user: User): UserResponseDto {
            return UserResponseDto(
                id = user.id,
                login = user.login,
                role = user.role.name,
                isActive = user.isActive,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt,
                lastLoginAt = user.lastLoginAt,
                lastInteractionAt = user.lastInteractionAt
            )
        }
    }
}
