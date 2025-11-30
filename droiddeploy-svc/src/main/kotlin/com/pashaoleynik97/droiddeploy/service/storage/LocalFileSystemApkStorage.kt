package com.pashaoleynik97.droiddeploy.service.storage

import com.pashaoleynik97.droiddeploy.core.config.StorageProperties
import com.pashaoleynik97.droiddeploy.core.exception.ApkNotFoundException
import com.pashaoleynik97.droiddeploy.core.exception.ApkStorageException
import com.pashaoleynik97.droiddeploy.core.storage.ApkPathResolver
import com.pashaoleynik97.droiddeploy.core.storage.ApkStorage
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

@Component
class LocalFileSystemApkStorage(
    private val storageProperties: StorageProperties
) : ApkStorage {

    private val rootPath: Path = Path.of(storageProperties.root)

    override fun saveApk(applicationId: UUID, versionCode: Long, input: InputStream) {
        try {
            val relative = ApkPathResolver.relativePath(applicationId, versionCode)
            val target = rootPath.resolve(relative)

            Files.createDirectories(target.parent)

            // Write to temp file first, then move atomically
            val tmpFile = Files.createTempFile(target.parent, "upload-", ".tmp")
            tmpFile.toFile().outputStream().use { out ->
                input.copyTo(out)
            }

            Files.move(tmpFile, target, StandardCopyOption.REPLACE_EXISTING)
        } catch (ex: IOException) {
            throw ApkStorageException("Failed to save APK file", ex)
        }
    }

    override fun loadApk(applicationId: UUID, versionCode: Long): InputStream {
        try {
            val relative = ApkPathResolver.relativePath(applicationId, versionCode)
            val target = rootPath.resolve(relative)

            if (!Files.exists(target)) {
                throw ApkNotFoundException("APK not found for application $applicationId and version $versionCode")
            }

            return Files.newInputStream(target)
        } catch (ex: ApkNotFoundException) {
            throw ex
        } catch (ex: IOException) {
            throw ApkStorageException("Failed to read APK file", ex)
        }
    }

    override fun deleteApk(applicationId: UUID, versionCode: Long) {
        try {
            val relative = ApkPathResolver.relativePath(applicationId, versionCode)
            val target = rootPath.resolve(relative)

            Files.deleteIfExists(target)
            // Optionally: cleanup empty parent dirs later
        } catch (ex: IOException) {
            throw ApkStorageException("Failed to delete APK file", ex)
        }
    }
}