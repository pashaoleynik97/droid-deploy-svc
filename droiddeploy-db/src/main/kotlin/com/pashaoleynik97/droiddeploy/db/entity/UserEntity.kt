package com.pashaoleynik97.droiddeploy.db.entity

import com.pashaoleynik97.droiddeploy.core.domain.User
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "\"user\"")
class UserEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "login", nullable = false, unique = true)
    var login: String,

    @Column(name = "password_hash")
    var passwordHash: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    var role: UserRole,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null,

    @Column(name = "last_interaction_at")
    var lastInteractionAt: Instant? = null,

    @Column(name = "token_version", nullable = false)
    var tokenVersion: Int = 0
) {
    fun toDomain(): User {
        return User(
            id = id,
            login = login,
            passwordHash = passwordHash,
            role = role,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastLoginAt = lastLoginAt,
            lastInteractionAt = lastInteractionAt,
            tokenVersion = tokenVersion
        )
    }

    companion object {
        fun fromDomain(user: User): UserEntity {
            return UserEntity(
                id = user.id,
                login = user.login,
                passwordHash = user.passwordHash,
                role = user.role,
                isActive = user.isActive,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt,
                lastLoginAt = user.lastLoginAt,
                lastInteractionAt = user.lastInteractionAt,
                tokenVersion = user.tokenVersion
            )
        }
    }
}
