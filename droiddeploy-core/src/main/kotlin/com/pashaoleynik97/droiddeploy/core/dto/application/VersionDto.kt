package com.pashaoleynik97.droiddeploy.core.dto.application

import com.pashaoleynik97.droiddeploy.core.domain.ApplicationVersion
import io.swagger.v3.oas.annotations.media.Schema

data class VersionDto(
    @Schema(description = "Human-readable version name from APK manifest", example = "1.2.3", nullable = true)
    val versionName: String?,
    @Schema(description = "Numeric version code from APK manifest", example = "123", nullable = true)
    val versionCode: Long?,
    @Schema(description = "Stability flag - true for stable releases, false for testing/beta versions", example = "true")
    val stable: Boolean
) {
    companion object {
        fun fromDomain(version: ApplicationVersion): VersionDto {
            return VersionDto(
                versionCode = version.versionCode.toLong(),
                versionName = version.versionName,
                stable = version.stable
            )
        }
    }
}