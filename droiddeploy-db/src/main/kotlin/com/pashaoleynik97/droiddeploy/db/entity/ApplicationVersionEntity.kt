package com.pashaoleynik97.droiddeploy.db.entity

import com.pashaoleynik97.droiddeploy.core.domain.ApplicationVersion
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "\"application_version\"")
class ApplicationVersionEntity(

    @Id
    val id: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    val application: ApplicationEntity,

    @Column(name = "version_name", length = 50)
    var versionName: String? = null,

    @Column(name = "version_code")
    var versionCode: Long? = null,

    @Column(name = "stable", nullable = false)
    var stable: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant

) {

    fun toDomain(): ApplicationVersion {
        return ApplicationVersion(
            id = id,
            application = application.toDomain(),
            versionName = versionName ?: "",
            versionCode = versionCode?.toInt() ?: 0,
            stable = stable,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(applicationVersion: ApplicationVersion, applicationEntity: ApplicationEntity): ApplicationVersionEntity {
            return ApplicationVersionEntity(
                id = applicationVersion.id,
                application = applicationEntity,
                versionName = applicationVersion.versionName,
                versionCode = applicationVersion.versionCode.toLong(),
                stable = applicationVersion.stable,
                createdAt = applicationVersion.createdAt
            )
        }
    }

}