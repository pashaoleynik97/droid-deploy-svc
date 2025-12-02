package com.pashaoleynik97.droiddeploy.integration

import com.pashaoleynik97.droiddeploy.AbstractIntegrationTest
import com.pashaoleynik97.droiddeploy.config.TestApkMetadataExtractorConfig
import com.pashaoleynik97.droiddeploy.core.config.StorageProperties
import com.pashaoleynik97.droiddeploy.core.domain.Application
import com.pashaoleynik97.droiddeploy.core.domain.User
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.core.repository.ApplicationRepository
import com.pashaoleynik97.droiddeploy.core.repository.UserRepository
import com.pashaoleynik97.droiddeploy.core.storage.ApkPathResolver
import com.pashaoleynik97.droiddeploy.db.entity.ApplicationVersionEntity
import com.pashaoleynik97.droiddeploy.db.repository.JpaApplicationRepository
import com.pashaoleynik97.droiddeploy.db.repository.JpaApplicationVersionRepository
import com.pashaoleynik97.droiddeploy.security.JwtTokenProvider
import com.pashaoleynik97.droiddeploy.service.application.ApkMetadata
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

@Import(TestApkMetadataExtractorConfig::class)
class ApplicationVersionControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var applicationRepository: ApplicationRepository

    @Autowired
    private lateinit var jpaApplicationRepository: JpaApplicationRepository

    @Autowired
    private lateinit var jpaApplicationVersionRepository: JpaApplicationVersionRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var testApkMetadataExtractor: TestApkMetadataExtractorConfig.TestApkMetadataExtractor

    @Autowired
    private lateinit var storageProperties: StorageProperties

    private lateinit var adminUser: User
    private lateinit var ciUser: User
    private lateinit var consumerUser: User
    private lateinit var adminAccessToken: String
    private lateinit var ciAccessToken: String
    private lateinit var consumerAccessToken: String

    @BeforeEach
    fun setUp() {
        // Build MockMvc with Spring Security
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()

        // Create unique test logins to avoid conflicts
        val uniqueId = System.currentTimeMillis()

        // Create test ADMIN user
        adminUser = User(
            id = UUID.randomUUID(),
            login = "admin_version_$uniqueId",
            passwordHash = passwordEncoder.encode("AdminPass123!"),
            role = UserRole.ADMIN,
            isActive = true,
            tokenVersion = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastLoginAt = null,
            lastInteractionAt = null
        )
        userRepository.save(adminUser)

        // Create test CI user
        ciUser = User(
            id = UUID.randomUUID(),
            login = "ci_version_$uniqueId",
            passwordHash = passwordEncoder.encode("CiPass123!"),
            role = UserRole.CI,
            isActive = true,
            tokenVersion = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastLoginAt = null,
            lastInteractionAt = null
        )
        userRepository.save(ciUser)

        // Create test CONSUMER user
        consumerUser = User(
            id = UUID.randomUUID(),
            login = "consumer_version_$uniqueId",
            passwordHash = passwordEncoder.encode("ConsumerPass123!"),
            role = UserRole.CONSUMER,
            isActive = true,
            tokenVersion = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastLoginAt = null,
            lastInteractionAt = null
        )
        userRepository.save(consumerUser)

        // Generate access tokens
        adminAccessToken = jwtTokenProvider.createAccessToken(adminUser.id, adminUser.role, adminUser.tokenVersion)
        ciAccessToken = jwtTokenProvider.createAccessToken(ciUser.id, ciUser.role, ciUser.tokenVersion)
        consumerAccessToken = jwtTokenProvider.createAccessToken(consumerUser.id, consumerUser.role, consumerUser.tokenVersion)
    }

    @Test
    fun `uploadVersion should return 201 when ADMIN uploads valid APK`() {
        // Given
        val application = createTestApplication("com.example.adminupload")
        val apkBytes = "fake apk content".toByteArray()
        val apkFile = MockMultipartFile("file", "test.apk", "application/vnd.android.package-archive", apkBytes)

        val metadata = ApkMetadata(
            versionCode = 1,
            versionName = "1.0.0",
            signingCertificateSha256 = "AABBCCDD112233445566778899"
        )

        testApkMetadataExtractor.setMetadata(metadata)

        // When & Then
        mockMvc.perform(
            multipart("/api/v1/application/{applicationId}/version", application.id)
                .file(apkFile)
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.versionCode").value(1))
            .andExpect(jsonPath("$.data.versionName").value("1.0.0"))
            .andExpect(jsonPath("$.data.stable").value(false))

        // Verify version exists in DB
        val versions = jpaApplicationVersionRepository.findAll()
        assertTrue(versions.any { it.application.id == application.id && it.versionCode == 1L })

        // Verify APK file exists on disk
        val apkPath = Path.of(storageProperties.root).resolve(ApkPathResolver.relativePath(application.id, 1L))
        assertTrue(Files.exists(apkPath), "APK file should exist at $apkPath")
    }

    @Test
    fun `uploadVersion should return 201 when CI uploads valid APK`() {
        // Given
        val application = createTestApplication("com.example.ciupload")
        val apkBytes = "fake apk content".toByteArray()
        val apkFile = MockMultipartFile("file", "test.apk", "application/vnd.android.package-archive", apkBytes)

        val metadata = ApkMetadata(
            versionCode = 1,
            versionName = "1.0.0",
            signingCertificateSha256 = "AABBCCDD112233445566778899"
        )

        testApkMetadataExtractor.setMetadata(metadata)

        // When & Then
        mockMvc.perform(
            multipart("/api/v1/application/{applicationId}/version", application.id)
                .file(apkFile)
                .header("Authorization", "Bearer $ciAccessToken")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.versionCode").value(1))
            .andExpect(jsonPath("$.data.versionName").value("1.0.0"))
            .andExpect(jsonPath("$.data.stable").value(false))
    }

    @Test
    fun `uploadVersion should return 403 when CONSUMER tries to upload`() {
        // Given
        val application = createTestApplication("com.example.consumerdenied")
        val apkBytes = "fake apk content".toByteArray()
        val apkFile = MockMultipartFile("file", "test.apk", "application/vnd.android.package-archive", apkBytes)

        // When & Then
        mockMvc.perform(
            multipart("/api/v1/application/{applicationId}/version", application.id)
                .file(apkFile)
                .header("Authorization", "Bearer $consumerAccessToken")
        )
            .andDo(print())
            .andExpect(status().isForbidden)
    }

    @Test
    fun `uploadVersion should return 404 when application not found`() {
        // Given
        val nonExistentId = UUID.randomUUID()
        val apkBytes = "fake apk content".toByteArray()
        val apkFile = MockMultipartFile("file", "test.apk", "application/vnd.android.package-archive", apkBytes)

        // When & Then
        mockMvc.perform(
            multipart("/api/v1/application/{applicationId}/version", nonExistentId)
                .file(apkFile)
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("NOT_FOUND"))
    }

    @Test
    fun `uploadVersion should set signing certificate on first upload`() {
        // Given
        val application = createTestApplication("com.example.firstcert")
        val apkBytes = "fake apk content".toByteArray()
        val apkFile = MockMultipartFile("file", "test.apk", "application/vnd.android.package-archive", apkBytes)

        val metadata = ApkMetadata(
            versionCode = 1,
            versionName = "1.0.0",
            signingCertificateSha256 = "FIRST_CERT_SHA256"
        )

        testApkMetadataExtractor.setMetadata(metadata)

        // When
        mockMvc.perform(
            multipart("/api/v1/application/{applicationId}/version", application.id)
                .file(apkFile)
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andExpect(status().isCreated)

        // Then - verify certificate was set
        val updatedApp = applicationRepository.findById(application.id)
        assertTrue(updatedApp != null && updatedApp.signingCertificateSha256 == "FIRST_CERT_SHA256")
    }

    @Test
    fun `uploadVersion should return 400 when signing certificate mismatches`() {
        // Given
        val application = createTestApplicationWithCertificate("com.example.certmismatch", "EXPECTED_CERT")
        val apkBytes = "fake apk content".toByteArray()
        val apkFile = MockMultipartFile("file", "test.apk", "application/vnd.android.package-archive", apkBytes)

        val metadata = ApkMetadata(
            versionCode = 1,
            versionName = "1.0.0",
            signingCertificateSha256 = "DIFFERENT_CERT"
        )

        testApkMetadataExtractor.setMetadata(metadata)

        // When & Then
        mockMvc.perform(
            multipart("/api/v1/application/{applicationId}/version", application.id)
                .file(apkFile)
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("VALIDATION"))
            .andExpect(jsonPath("$.errors[0].message").exists())
    }

    @Test
    fun `uploadVersion should return 409 when version code already exists`() {
        // Given
        val application = createTestApplication("com.example.duplicateversion")
        createExistingVersion(application.id, versionCode = 5)

        val apkBytes = "fake apk content".toByteArray()
        val apkFile = MockMultipartFile("file", "test.apk", "application/vnd.android.package-archive", apkBytes)

        val metadata = ApkMetadata(
            versionCode = 5,
            versionName = "2.0.0",
            signingCertificateSha256 = "CERT_SHA256"
        )

        testApkMetadataExtractor.setMetadata(metadata)

        // When & Then
        mockMvc.perform(
            multipart("/api/v1/application/{applicationId}/version", application.id)
                .file(apkFile)
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andDo(print())
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("VALIDATION"))
    }

    @Test
    fun `uploadVersion should return 400 when version code is not greater than max`() {
        // Given
        val application = createTestApplication("com.example.nonincreasing")
        createExistingVersion(application.id, versionCode = 10)

        val apkBytes = "fake apk content".toByteArray()
        val apkFile = MockMultipartFile("file", "test.apk", "application/vnd.android.package-archive", apkBytes)

        val metadata = ApkMetadata(
            versionCode = 5,
            versionName = "0.5.0",
            signingCertificateSha256 = "CERT_SHA256"
        )

        testApkMetadataExtractor.setMetadata(metadata)

        // When & Then
        mockMvc.perform(
            multipart("/api/v1/application/{applicationId}/version", application.id)
                .file(apkFile)
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("VALIDATION"))
    }

    @Test
    fun `uploadVersion should accept increasing version codes`() {
        // Given
        val application = createTestApplication("com.example.increasing")
        createExistingVersion(application.id, versionCode = 5)
        createExistingVersion(application.id, versionCode = 10)

        val apkBytes = "fake apk content".toByteArray()
        val apkFile = MockMultipartFile("file", "test.apk", "application/vnd.android.package-archive", apkBytes)

        val metadata = ApkMetadata(
            versionCode = 15,
            versionName = "1.5.0",
            signingCertificateSha256 = "CERT_SHA256"
        )

        testApkMetadataExtractor.setMetadata(metadata)

        // When & Then
        mockMvc.perform(
            multipart("/api/v1/application/{applicationId}/version", application.id)
                .file(apkFile)
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.versionCode").value(15))
    }

    @Test
    fun `updateVersionStability should return 200 when ADMIN updates version to stable`() {
        // Given
        val application = createTestApplication("com.example.adminstable")
        createExistingVersion(application.id, versionCode = 10)

        val requestBody = """{"stable": true}"""

        // When & Then
        mockMvc.perform(
            put("/api/v1/application/{applicationId}/version/{versionCode}", application.id, 10)
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.versionCode").value(10))
            .andExpect(jsonPath("$.data.stable").value(true))
            .andExpect(jsonPath("$.message").value("Version stability updated successfully"))

        // Verify in DB
        val versionEntity = jpaApplicationVersionRepository.findByApplicationIdAndVersionCode(application.id, 10)
        assertTrue(versionEntity != null && versionEntity.stable)
    }

    @Test
    fun `updateVersionStability should return 200 when CI updates version to stable`() {
        // Given
        val application = createTestApplication("com.example.cistable")
        createExistingVersion(application.id, versionCode = 5)

        val requestBody = """{"stable": true}"""

        // When & Then
        mockMvc.perform(
            put("/api/v1/application/{applicationId}/version/{versionCode}", application.id, 5)
                .header("Authorization", "Bearer $ciAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.versionCode").value(5))
            .andExpect(jsonPath("$.data.stable").value(true))
    }

    @Test
    fun `updateVersionStability should return 200 when updating version to unstable`() {
        // Given
        val application = createTestApplication("com.example.unstable")
        createExistingVersionWithStability(application.id, versionCode = 7, stable = true)

        val requestBody = """{"stable": false}"""

        // When & Then
        mockMvc.perform(
            put("/api/v1/application/{applicationId}/version/{versionCode}", application.id, 7)
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.versionCode").value(7))
            .andExpect(jsonPath("$.data.stable").value(false))

        // Verify in DB
        val versionEntity = jpaApplicationVersionRepository.findByApplicationIdAndVersionCode(application.id, 7)
        assertTrue(versionEntity != null && !versionEntity.stable)
    }

    @Test
    fun `updateVersionStability should return 403 when CONSUMER tries to update`() {
        // Given
        val application = createTestApplication("com.example.consumerdeniedstable")
        createExistingVersion(application.id, versionCode = 3)

        val requestBody = """{"stable": true}"""

        // When & Then
        mockMvc.perform(
            put("/api/v1/application/{applicationId}/version/{versionCode}", application.id, 3)
                .header("Authorization", "Bearer $consumerAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andDo(print())
            .andExpect(status().isForbidden)
    }

    @Test
    fun `updateVersionStability should return 404 when application not found`() {
        // Given
        val nonExistentApplicationId = UUID.randomUUID()
        val requestBody = """{"stable": true}"""

        // When & Then
        mockMvc.perform(
            put("/api/v1/application/{applicationId}/version/{versionCode}", nonExistentApplicationId, 1)
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("NOT_FOUND"))
    }

    @Test
    fun `updateVersionStability should return 404 when version not found`() {
        // Given
        val application = createTestApplication("com.example.versionnotfound")
        val requestBody = """{"stable": true}"""

        // When & Then
        mockMvc.perform(
            put("/api/v1/application/{applicationId}/version/{versionCode}", application.id, 999)
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("NOT_FOUND"))
    }

    @Test
    fun `updateVersionStability should allow toggling stability back and forth`() {
        // Given
        val application = createTestApplication("com.example.toggle")
        createExistingVersion(application.id, versionCode = 20)

        // When - set to stable
        mockMvc.perform(
            put("/api/v1/application/{applicationId}/version/{versionCode}", application.id, 20)
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"stable": true}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.stable").value(true))

        // Then - set back to unstable
        mockMvc.perform(
            put("/api/v1/application/{applicationId}/version/{versionCode}", application.id, 20)
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"stable": false}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.stable").value(false))

        // And - set to stable again
        mockMvc.perform(
            put("/api/v1/application/{applicationId}/version/{versionCode}", application.id, 20)
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"stable": true}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.stable").value(true))
    }

    @Test
    fun `deleteVersion should return 200 when ADMIN deletes version`() {
        // Given
        val application = createTestApplication("com.example.admindelete")
        createExistingVersion(application.id, versionCode = 15)

        // When & Then
        mockMvc.perform(
            delete("/api/v1/application/{applicationId}/version/{versionCode}", application.id, 15)
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Deleted"))

        // Verify version was deleted from DB
        val versionEntity = jpaApplicationVersionRepository.findByApplicationIdAndVersionCode(application.id, 15)
        assertTrue(versionEntity == null, "Version should be deleted from database")
    }

    @Test
    fun `deleteVersion should return 403 when CI tries to delete`() {
        // Given
        val application = createTestApplication("com.example.cidelete")
        createExistingVersion(application.id, versionCode = 10)

        // When & Then
        mockMvc.perform(
            delete("/api/v1/application/{applicationId}/version/{versionCode}", application.id, 10)
                .header("Authorization", "Bearer $ciAccessToken")
        )
            .andDo(print())
            .andExpect(status().isForbidden)

        // Verify version still exists
        val versionEntity = jpaApplicationVersionRepository.findByApplicationIdAndVersionCode(application.id, 10)
        assertTrue(versionEntity != null, "Version should not be deleted")
    }

    @Test
    fun `deleteVersion should return 403 when CONSUMER tries to delete`() {
        // Given
        val application = createTestApplication("com.example.consumerdelete")
        createExistingVersion(application.id, versionCode = 8)

        // When & Then
        mockMvc.perform(
            delete("/api/v1/application/{applicationId}/version/{versionCode}", application.id, 8)
                .header("Authorization", "Bearer $consumerAccessToken")
        )
            .andDo(print())
            .andExpect(status().isForbidden)

        // Verify version still exists
        val versionEntity = jpaApplicationVersionRepository.findByApplicationIdAndVersionCode(application.id, 8)
        assertTrue(versionEntity != null, "Version should not be deleted")
    }

    @Test
    fun `deleteVersion should return 404 when application not found`() {
        // Given
        val nonExistentApplicationId = UUID.randomUUID()

        // When & Then
        mockMvc.perform(
            delete("/api/v1/application/{applicationId}/version/{versionCode}", nonExistentApplicationId, 1)
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("NOT_FOUND"))
    }

    @Test
    fun `deleteVersion should return 404 when version not found`() {
        // Given
        val application = createTestApplication("com.example.versionnotfounddelete")

        // When & Then
        mockMvc.perform(
            delete("/api/v1/application/{applicationId}/version/{versionCode}", application.id, 999)
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("NOT_FOUND"))
    }

    @Test
    fun `deleteVersion should delete APK file from storage`() {
        // Given
        val application = createTestApplication("com.example.deletewithapk")
        val apkBytes = "fake apk content".toByteArray()
        val apkFile = MockMultipartFile("file", "test.apk", "application/vnd.android.package-archive", apkBytes)

        val metadata = ApkMetadata(
            versionCode = 25,
            versionName = "2.5.0",
            signingCertificateSha256 = "CERT_SHA256_DELETE"
        )

        testApkMetadataExtractor.setMetadata(metadata)

        // Upload version first
        mockMvc.perform(
            multipart("/api/v1/application/{applicationId}/version", application.id)
                .file(apkFile)
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andExpect(status().isCreated)

        // Verify APK file exists
        val apkPath = Path.of(storageProperties.root).resolve(ApkPathResolver.relativePath(application.id, 25L))
        assertTrue(Files.exists(apkPath), "APK file should exist before deletion")

        // When - delete version
        mockMvc.perform(
            delete("/api/v1/application/{applicationId}/version/{versionCode}", application.id, 25)
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andExpect(status().isOk)

        // Then - verify APK file was deleted
        assertTrue(!Files.exists(apkPath), "APK file should be deleted from storage")
    }

    @Test
    fun `getVersion should return 200 with correct VersionDto when ADMIN requests`() {
        // Given
        val application = createTestApplication("com.example.getversion")
        createExistingVersionWithStability(application.id, 42, stable = true)

        // When & Then
        mockMvc.perform(
            get("/api/v1/application/{applicationId}/version/{versionCode}", application.id, 42)
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Version retrieved successfully"))
            .andExpect(jsonPath("$.data.versionCode").value(42))
            .andExpect(jsonPath("$.data.versionName").value("Version 42"))
            .andExpect(jsonPath("$.data.stable").value(true))
    }

    @Test
    fun `getVersion should return 403 when non-ADMIN requests`() {
        // Given
        val application = createTestApplication("com.example.getversionforbidden")
        createExistingVersion(application.id, 10)

        // When & Then - CI user
        mockMvc.perform(
            get("/api/v1/application/{applicationId}/version/{versionCode}", application.id, 10)
                .header("Authorization", "Bearer $ciAccessToken")
        )
            .andDo(print())
            .andExpect(status().isForbidden)

        // When & Then - CONSUMER user
        mockMvc.perform(
            get("/api/v1/application/{applicationId}/version/{versionCode}", application.id, 10)
                .header("Authorization", "Bearer $consumerAccessToken")
        )
            .andDo(print())
            .andExpect(status().isForbidden)
    }

    @Test
    fun `getVersion should return 404 when application not found`() {
        // Given
        val nonExistentApplicationId = UUID.randomUUID()

        // When & Then
        mockMvc.perform(
            get("/api/v1/application/{applicationId}/version/{versionCode}", nonExistentApplicationId, 1)
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("NOT_FOUND"))
    }

    @Test
    fun `getVersion should return 404 when version not found`() {
        // Given
        val application = createTestApplication("com.example.versionnotfoundget")

        // When & Then
        mockMvc.perform(
            get("/api/v1/application/{applicationId}/version/{versionCode}", application.id, 999)
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("NOT_FOUND"))
    }

    @Test
    fun `listVersions should return 200 with paged response when ADMIN requests`() {
        // Given
        val application = createTestApplication("com.example.listversions")
        createExistingVersion(application.id, 1)
        createExistingVersion(application.id, 2)
        createExistingVersion(application.id, 3)

        // When & Then
        mockMvc.perform(
            get("/api/v1/application/{applicationId}/version", application.id)
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Versions retrieved successfully"))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content.length()").value(3))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(20))
            .andExpect(jsonPath("$.data.totalElements").value(3))
            .andExpect(jsonPath("$.data.totalPages").value(1))
    }

    @Test
    fun `listVersions should return versions sorted by createdAt descending`() {
        // Given
        val application = createTestApplication("com.example.listversionssorted")
        Thread.sleep(10)
        createExistingVersion(application.id, 1)
        Thread.sleep(10)
        createExistingVersion(application.id, 2)
        Thread.sleep(10)
        createExistingVersion(application.id, 3)

        // When & Then - newest version should be first
        mockMvc.perform(
            get("/api/v1/application/{applicationId}/version", application.id)
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].versionCode").value(3))
            .andExpect(jsonPath("$.data.content[1].versionCode").value(2))
            .andExpect(jsonPath("$.data.content[2].versionCode").value(1))
    }

    @Test
    fun `listVersions should respect page size limit of 100`() {
        // Given
        val application = createTestApplication("com.example.listversionsmaxsize")
        createExistingVersion(application.id, 1)

        // When & Then - request size of 200, should be capped at 100
        mockMvc.perform(
            get("/api/v1/application/{applicationId}/version", application.id)
                .param("page", "0")
                .param("size", "200")
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.size").value(100)) // Page size capped at 100
            .andExpect(jsonPath("$.data.content.length()").value(1)) // Only 1 version exists
    }

    @Test
    fun `listVersions should return 403 when non-ADMIN requests`() {
        // Given
        val application = createTestApplication("com.example.listversionsforbidden")
        createExistingVersion(application.id, 1)

        // When & Then - CI user
        mockMvc.perform(
            get("/api/v1/application/{applicationId}/version", application.id)
                .header("Authorization", "Bearer $ciAccessToken")
        )
            .andDo(print())
            .andExpect(status().isForbidden)

        // When & Then - CONSUMER user
        mockMvc.perform(
            get("/api/v1/application/{applicationId}/version", application.id)
                .header("Authorization", "Bearer $consumerAccessToken")
        )
            .andDo(print())
            .andExpect(status().isForbidden)
    }

    @Test
    fun `listVersions should return 404 when application not found`() {
        // Given
        val nonExistentApplicationId = UUID.randomUUID()

        // When & Then
        mockMvc.perform(
            get("/api/v1/application/{applicationId}/version", nonExistentApplicationId)
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("NOT_FOUND"))
    }

    @Test
    fun `listVersions should handle pagination correctly`() {
        // Given
        val application = createTestApplication("com.example.listversionspagination")
        for (i in 1..5) {
            Thread.sleep(10)
            createExistingVersion(application.id, i)
        }

        // When & Then - page 0, size 2
        mockMvc.perform(
            get("/api/v1/application/{applicationId}/version", application.id)
                .param("page", "0")
                .param("size", "2")
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(2))
            .andExpect(jsonPath("$.data.totalElements").value(5))
            .andExpect(jsonPath("$.data.totalPages").value(3))
            .andExpect(jsonPath("$.data.content[0].versionCode").value(5)) // Newest first
            .andExpect(jsonPath("$.data.content[1].versionCode").value(4))

        // When & Then - page 1, size 2
        mockMvc.perform(
            get("/api/v1/application/{applicationId}/version", application.id)
                .param("page", "1")
                .param("size", "2")
                .header("Authorization", "Bearer $adminAccessToken")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.page").value(1))
            .andExpect(jsonPath("$.data.content[0].versionCode").value(3))
            .andExpect(jsonPath("$.data.content[1].versionCode").value(2))
    }

    // Helper methods

    private fun createTestApplication(bundleId: String): Application {
        val application = Application(
            id = UUID.randomUUID(),
            name = "Test App $bundleId",
            bundleId = bundleId,
            signingCertificateSha256 = null,
            createdAt = Instant.now().toEpochMilli()
        )
        return applicationRepository.save(application)
    }

    private fun createTestApplicationWithCertificate(bundleId: String, certificateSha256: String): Application {
        val application = Application(
            id = UUID.randomUUID(),
            name = "Test App $bundleId",
            bundleId = bundleId,
            signingCertificateSha256 = certificateSha256,
            createdAt = Instant.now().toEpochMilli()
        )
        return applicationRepository.save(application)
    }

    private fun createExistingVersion(applicationId: UUID, versionCode: Int) {
        val applicationEntity = jpaApplicationRepository.findById(applicationId).orElseThrow()
        val versionEntity = ApplicationVersionEntity(
            id = UUID.randomUUID(),
            application = applicationEntity,
            versionName = "Version $versionCode",
            versionCode = versionCode.toLong(),
            stable = false,
            createdAt = Instant.now()
        )
        jpaApplicationVersionRepository.save(versionEntity)
    }

    private fun createExistingVersionWithStability(applicationId: UUID, versionCode: Int, stable: Boolean) {
        val applicationEntity = jpaApplicationRepository.findById(applicationId).orElseThrow()
        val versionEntity = ApplicationVersionEntity(
            id = UUID.randomUUID(),
            application = applicationEntity,
            versionName = "Version $versionCode",
            versionCode = versionCode.toLong(),
            stable = stable,
            createdAt = Instant.now()
        )
        jpaApplicationVersionRepository.save(versionEntity)
    }
}
