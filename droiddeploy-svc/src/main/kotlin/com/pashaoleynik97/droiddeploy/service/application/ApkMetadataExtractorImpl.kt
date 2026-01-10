package com.pashaoleynik97.droiddeploy.service.application

import mu.KotlinLogging
import net.dongliu.apk.parser.ByteArrayApkFile
import org.springframework.stereotype.Component
import java.security.MessageDigest

private val logger = KotlinLogging.logger {}

/**
 * Implementation of ApkMetadataExtractor using apk-parser library.
 *
 * This extracts:
 * 1. versionCode and versionName from AndroidManifest.xml
 * 2. Signing certificate SHA-256 fingerprint from META-INF/
 */
@Component
class ApkMetadataExtractorImpl : ApkMetadataExtractor {

    override fun extractMetadata(apkContent: ByteArray): ApkMetadata {
        logger.debug { "Extracting metadata from APK file (${apkContent.size} bytes)" }

        return try {
            ByteArrayApkFile(apkContent).use { apkFile ->
                // Extract version info from AndroidManifest.xml
                val apkMeta = apkFile.apkMeta
                val versionCode = apkMeta.versionCode.toInt()
                val versionName = apkMeta.versionName ?: "unknown"

                logger.trace { "Extracted version: $versionName ($versionCode)" }

                // Extract signing certificate SHA-256 fingerprint using apk-parser library
                // If APK is unsigned, use a placeholder value (for testing/debugging APKs)
                val signingCertificateSha256 = try {
                    extractSigningCertificateSha256(apkFile)
                } catch (e: Exception) {
                    logger.warn { "Failed to extract signing certificate (APK may be unsigned): ${e.message}" }
                    // Return placeholder for unsigned/debug APKs
                    "0".repeat(64) // 64 zeros as placeholder SHA-256
                }

                logger.debug { "Successfully extracted APK metadata" }

                ApkMetadata(
                    versionCode = versionCode,
                    versionName = versionName,
                    signingCertificateSha256 = signingCertificateSha256
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to extract APK metadata" }
            throw IllegalArgumentException("Failed to parse APK file: ${e.message}", e)
        }
    }

    private fun extractSigningCertificateSha256(apkFile: ByteArrayApkFile): String {
        // Try to get certificate info from the APK (supports both v1 and v2/v3 signing schemes)
        @Suppress("DEPRECATION")
        val certificates = apkFile.certificateMetaList

        if (certificates.isNullOrEmpty()) {
            throw IllegalArgumentException("No signing certificates found in APK")
        }

        // Get the first certificate (signer certificate)
        val certMeta = certificates.first()

        // Get the raw certificate data
        val certData = certMeta.data

        // Compute SHA256 fingerprint
        val sha256 = MessageDigest.getInstance("SHA-256")
        val fingerprint = sha256.digest(certData)

        return fingerprint.joinToString("") { "%02X".format(it) }
    }
}
