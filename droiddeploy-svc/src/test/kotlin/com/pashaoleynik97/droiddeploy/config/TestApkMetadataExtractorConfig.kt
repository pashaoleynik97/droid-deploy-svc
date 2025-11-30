package com.pashaoleynik97.droiddeploy.config

import com.pashaoleynik97.droiddeploy.service.application.ApkMetadata
import com.pashaoleynik97.droiddeploy.service.application.ApkMetadataExtractor
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestApkMetadataExtractorConfig {

    @Bean
    @Primary
    fun testApkMetadataExtractor(): ApkMetadataExtractor {
        return TestApkMetadataExtractor()
    }

    class TestApkMetadataExtractor : ApkMetadataExtractor {
        // Store metadata to be returned for each test
        private var metadataToReturn: ApkMetadata? = null

        fun setMetadata(metadata: ApkMetadata) {
            metadataToReturn = metadata
        }

        override fun extractMetadata(apkContent: ByteArray): ApkMetadata {
            return metadataToReturn
                ?: throw IllegalStateException("Test metadata not configured. Call setMetadata() first.")
        }
    }
}
