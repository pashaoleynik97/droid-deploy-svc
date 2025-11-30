package com.pashaoleynik97.droiddeploy.core.domain

import java.time.Instant
import java.util.UUID

data class ApplicationVersion(
    val id: UUID,
    val application: Application,
    val versionName: String,
    val versionCode: Int,
    val stable: Boolean,
    val createdAt: Instant
)