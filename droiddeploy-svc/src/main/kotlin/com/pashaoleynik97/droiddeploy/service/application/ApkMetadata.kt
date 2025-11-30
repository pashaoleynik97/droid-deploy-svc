package com.pashaoleynik97.droiddeploy.service.application

data class ApkMetadata(
    val versionCode: Int,
    val versionName: String,
    val signingCertificateSha256: String
)
