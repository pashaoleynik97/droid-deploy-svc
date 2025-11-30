package com.pashaoleynik97.droiddeploy.db.entity

import com.pashaoleynik97.droiddeploy.core.domain.Application
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "\"application\"")
class ApplicationEntity(
    @Id
    val id: UUID,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(name = "bundle_id", nullable = false, length = 255, unique = true)
    var bundleId: String,

    @Column(name = "signing_certificate_sha256", length = 64)
    var signingCertificateSha256: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant
) {

    fun toDomain(): Application {
        return Application(
            id = id,
            name = name,
            bundleId = bundleId,
            signingCertificateSha256 = signingCertificateSha256,
            createdAt = createdAt.toEpochMilli()
        )
    }

    companion object {
        fun fromDomain(application: Application): ApplicationEntity {
            return ApplicationEntity(
                id = application.id,
                name = application.name,
                bundleId = application.bundleId,
                signingCertificateSha256 = application.signingCertificateSha256,
                createdAt = Instant.ofEpochMilli(application.createdAt)
            )
        }
    }
}