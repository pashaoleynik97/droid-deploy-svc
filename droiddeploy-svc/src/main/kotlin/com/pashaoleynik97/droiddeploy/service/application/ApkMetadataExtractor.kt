package com.pashaoleynik97.droiddeploy.service.application

interface ApkMetadataExtractor {
    /**
     * Extracts metadata from an APK file.
     *
     * @param apkContent the APK file content as byte array
     * @return extracted metadata including versionCode, versionName, and signing certificate SHA256
     * @throws IllegalArgumentException if the APK is invalid or cannot be parsed
     */
    fun extractMetadata(apkContent: ByteArray): ApkMetadata
}
