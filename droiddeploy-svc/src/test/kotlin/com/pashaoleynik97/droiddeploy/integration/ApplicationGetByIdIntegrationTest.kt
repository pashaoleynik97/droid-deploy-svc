package com.pashaoleynik97.droiddeploy.integration

import com.pashaoleynik97.droiddeploy.AbstractIntegrationTest
import com.pashaoleynik97.droiddeploy.core.domain.Application
import com.pashaoleynik97.droiddeploy.core.domain.User
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.core.repository.ApplicationRepository
import com.pashaoleynik97.droiddeploy.core.repository.UserRepository
import com.pashaoleynik97.droiddeploy.security.JwtTokenProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant
import java.util.UUID

class ApplicationGetByIdIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var applicationRepository: ApplicationRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var adminUser: User
    private lateinit var adminAccessToken: String
    private lateinit var ciAccessToken: String
    private lateinit var testApplication: Application

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
            login = "admin_app_get_$uniqueId",
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
        val ciUser = User(
            id = UUID.randomUUID(),
            login = "ci_app_get_$uniqueId",
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

        // Create test application
        testApplication = Application(
            id = UUID.randomUUID(),
            name = "Test Application",
            bundleId = "com.example.testapp.$uniqueId",
            signingCertificateSha256 = null,
            createdAt = Instant.now().toEpochMilli()
        )
        applicationRepository.save(testApplication)

        // Generate access tokens
        adminAccessToken = jwtTokenProvider.createAccessToken(adminUser.id, adminUser.role, adminUser.tokenVersion)
        ciAccessToken = jwtTokenProvider.createAccessToken(ciUser.id, ciUser.role, ciUser.tokenVersion)
    }

    @Test
    fun `GET application by ID should return 200 when application exists`() {
        mockMvc.perform(
            get("/api/v1/application/{id}", testApplication.id)
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(testApplication.id.toString()))
            .andExpect(jsonPath("$.data.name").value(testApplication.name))
            .andExpect(jsonPath("$.data.bundleId").value(testApplication.bundleId))
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.message").value("Application retrieved successfully"))
    }

    @Test
    fun `GET application by ID should return 404 when application not found`() {
        val nonExistentId = UUID.randomUUID()

        mockMvc.perform(
            get("/api/v1/application/{id}", nonExistentId)
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.errors[0].type").value("NOT_FOUND"))
            .andExpect(jsonPath("$.errors[0].message").value("Application with ID '$nonExistentId' not found"))
    }

    @Test
    fun `GET application by ID should return 403 when accessed by non-ADMIN user`() {
        mockMvc.perform(
            get("/api/v1/application/{id}", testApplication.id)
                .header("Authorization", "Bearer $ciAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET application by ID should return 403 when no authentication provided`() {
        mockMvc.perform(
            get("/api/v1/application/{id}", testApplication.id)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET application by ID should return 403 when invalid token is provided`() {
        mockMvc.perform(
            get("/api/v1/application/{id}", testApplication.id)
                .header("Authorization", "Bearer invalid_token_here")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isForbidden)
    }
}
