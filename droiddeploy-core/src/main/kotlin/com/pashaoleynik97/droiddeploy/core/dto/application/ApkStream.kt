package com.pashaoleynik97.droiddeploy.core.dto.application

import java.io.InputStream

/**
 * Represents an APK file stream with its metadata for download operations
 */
data class ApkStream(
    val inputStream: InputStream,
    val fileName: String
)
