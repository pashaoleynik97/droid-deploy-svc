package com.pashaoleynik97.droiddeploy.service.application

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class ApkMetadataExtractorImplTest {

    private lateinit var apkMetadataExtractor: ApkMetadataExtractor

    @BeforeEach
    fun setUp() {
        apkMetadataExtractor = ApkMetadataExtractorImpl()
    }

    @Test
    fun `extractMetadata should successfully extract metadata from real APK`() {
        // Given
        val apkResource = ClassPathResource("apk/app-release.apk")
        val apkContent = apkResource.inputStream.readBytes()

        // When
        val metadata = apkMetadataExtractor.extractMetadata(apkContent)

        // Then
        assertNotNull(metadata)
        assertTrue(metadata.versionCode > 0, "Version code should be positive")
        assertNotNull(metadata.versionName)
        assertTrue(metadata.versionName.isNotBlank(), "Version name should not be blank")
        assertNotNull(metadata.signingCertificateSha256)
        assertTrue(metadata.signingCertificateSha256.matches(Regex("^[A-F0-9]{64}$")),
            "Signing certificate SHA-256 should be 64 hex characters (or 64 zeros for unsigned APKs)")
    }

    @Test
    fun `extractMetadata should extract consistent metadata from same APK`() {
        // Given
        val apkResource = ClassPathResource("apk/app-release.apk")
        val apkContent = apkResource.inputStream.readBytes()

        // When
        val metadata1 = apkMetadataExtractor.extractMetadata(apkContent)
        val metadata2 = apkMetadataExtractor.extractMetadata(apkContent)

        // Then
        assertEquals(metadata1.versionCode, metadata2.versionCode)
        assertEquals(metadata1.versionName, metadata2.versionName)
        assertEquals(metadata1.signingCertificateSha256, metadata2.signingCertificateSha256)
    }

    @Test
    fun `extractMetadata should throw IllegalArgumentException for invalid APK`() {
        // Given
        val invalidApkContent = "not a valid APK file".toByteArray()

        // When/Then
        assertThrows(IllegalArgumentException::class.java) {
            apkMetadataExtractor.extractMetadata(invalidApkContent)
        }
    }

    @Test
    fun `extractMetadata should throw IllegalArgumentException for empty content`() {
        // Given
        val emptyContent = ByteArray(0)

        // When/Then
        assertThrows(IllegalArgumentException::class.java) {
            apkMetadataExtractor.extractMetadata(emptyContent)
        }
    }
}
