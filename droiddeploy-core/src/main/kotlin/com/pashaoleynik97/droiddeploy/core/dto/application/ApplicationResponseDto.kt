package com.pashaoleynik97.droiddeploy.core.dto.application

import com.pashaoleynik97.droiddeploy.core.domain.Application
import java.time.Instant
import java.util.UUID

data class ApplicationResponseDto(
    val id: UUID,
    val name: String,
    val bundleId: String,
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