package com.pashaoleynik97.droiddeploy.core.storage

import java.io.InputStream
import java.util.UUID

interface ApkStorage {

    /**
     * Saves the APK content for the given application and version.
     * Overwrites existing file if already present.
     */
    fun saveApk(applicationId: UUID, versionCode: Long, input: InputStream)

    /**
     * Returns an InputStream to read the APK content.
     * Throws an appropriate exception if file does not exist.
     */
    fun loadApk(applicationId: UUID, versionCode: Long): InputStream

    /**
     * Deletes APK for the given application and version if exists.
     * Should be idempotent.
     */
    fun deleteApk(applicationId: UUID, versionCode: Long)

}