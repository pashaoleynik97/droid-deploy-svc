package com.pashaoleynik97.droiddeploy.db.entity

import com.pashaoleynik97.droiddeploy.core.domain.ApiKey
import com.pashaoleynik97.droiddeploy.core.domain.ApiKeyRole
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "api_key")
class ApiKeyEntity(
    @Id
    val id: UUID,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(name = "value_hash", nullable = false, length = 64)
    val valueHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var role: ApiKeyRole,

    @Column(name = "application_id", nullable = false)
    val applicationId: UUID,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "last_used_at")
    var lastUsedAt: Instant?,

    @Column(name = "expires_at")
    val expiresAt: Instant?,

    @Column(name = "token_version", nullable = false)
    val tokenVersion: Int = 0
) {

    fun toDomain(): ApiKey {
        return ApiKey(
            id = id,
            name = name,
            valueHash = valueHash,
            role = role,
            applicationId = applicationId,
            isActive = isActive,
            createdAt = createdAt.toEpochMilli(),
            lastUsedAt = lastUsedAt?.toEpochMilli(),
            expiresAt = expiresAt?.toEpochMilli(),
            tokenVersion = tokenVersion
        )
    }

    companion object {
        fun fromDomain(apiKey: ApiKey): ApiKeyEntity {
            return ApiKeyEntity(
                id = apiKey.id,
                name = apiKey.name,
                valueHash = apiKey.valueHash,
                role = apiKey.role,
                applicationId = apiKey.applicationId,
                isActive = apiKey.isActive,
                createdAt = Instant.ofEpochMilli(apiKey.createdAt),
                lastUsedAt = apiKey.lastUsedAt?.let { Instant.ofEpochMilli(it) },
                expiresAt = apiKey.expiresAt?.let { Instant.ofEpochMilli(it) },
                tokenVersion = apiKey.tokenVersion
            )
        }
    }
}
