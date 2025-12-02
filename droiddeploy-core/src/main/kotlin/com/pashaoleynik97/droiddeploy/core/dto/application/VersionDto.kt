package com.pashaoleynik97.droiddeploy.core.dto.application

import com.pashaoleynik97.droiddeploy.core.domain.ApplicationVersion

data class VersionDto(
    val versionName: String?,
    val versionCode: Long?,
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