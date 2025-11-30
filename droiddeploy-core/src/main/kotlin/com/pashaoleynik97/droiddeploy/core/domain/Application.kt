package com.pashaoleynik97.droiddeploy.core.domain

import java.util.UUID

data class Application(
    val id: UUID,
    val name: String,
    val bundleId: String,
    val signingCertificateSha256: String?,
    val createdAt: Long
)