package com.pashaoleynik97.droiddeploy.core.dto.user

import com.pashaoleynik97.droiddeploy.core.domain.User
import java.time.Instant
import java.util.UUID

data class UserResponseDto(
    val id: UUID,
    val login: String,
    val role: String,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastLoginAt: Instant?,
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
