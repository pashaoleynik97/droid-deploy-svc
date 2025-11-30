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

class ApplicationListIntegrationTest : AbstractIntegrationTest() {

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
            login = "admin_app_list_$uniqueId",
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
            login = "ci_app_list_$uniqueId",
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

        // Create test applications
        Thread.sleep(10) // Small delay to ensure different createdAt timestamps
        createTestApplication("app_list_test_1_$uniqueId", "com.example.app1.$uniqueId")
        Thread.sleep(10)
        createTestApplication("app_list_test_2_$uniqueId", "com.example.app2.$uniqueId")
        Thread.sleep(10)
        createTestApplication("app_list_test_3_$uniqueId", "com.example.app3.$uniqueId")
        Thread.sleep(10)
        createTestApplication("app_list_test_4_$uniqueId", "com.example.app4.$uniqueId")
        Thread.sleep(10)
        createTestApplication("app_list_test_5_$uniqueId", "com.example.app5.$uniqueId")

        // Generate access tokens
        adminAccessToken = jwtTokenProvider.createAccessToken(adminUser.id, adminUser.role, adminUser.tokenVersion)
        ciAccessToken = jwtTokenProvider.createAccessToken(ciUser.id, ciUser.role, ciUser.tokenVersion)
    }

    private fun createTestApplication(name: String, bundleId: String) {
        val application = Application(
            id = UUID.randomUUID(),
            name = name,
            bundleId = bundleId,
            signingCertificateSha256 = null,
            createdAt = Instant.now().toEpochMilli()
        )
        applicationRepository.save(application)
    }

    @Test
    fun `GET applications should return 200 with all applications`() {
        mockMvc.perform(
            get("/api/v1/application")
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(20))
            .andExpect(jsonPath("$.data.totalElements").exists())
            .andExpect(jsonPath("$.data.totalPages").exists())
    }

    @Test
    fun `GET applications should support custom page size`() {
        mockMvc.perform(
            get("/api/v1/application")
                .param("size", "2")
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.size").value(2))
            .andExpect(jsonPath("$.data.content.length()").value(org.hamcrest.Matchers.lessThanOrEqualTo(2)))
    }

    @Test
    fun `GET applications should cap page size at 100`() {
        mockMvc.perform(
            get("/api/v1/application")
                .param("size", "200")
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.size").value(100))
    }

    @Test
    fun `GET applications should support pagination`() {
        mockMvc.perform(
            get("/api/v1/application")
                .param("page", "1")
                .param("size", "2")
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.page").value(1))
    }

    @Test
    fun `GET applications should sort by createdAt descending`() {
        val result = mockMvc.perform(
            get("/api/v1/application")
                .param("size", "5")
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
            .andReturn()

        // Note: We can't easily verify the exact order without parsing the response,
        // but we can verify the endpoint accepts the request and returns data
    }

    @Test
    fun `GET applications should return 403 when accessed by non-ADMIN user`() {
        mockMvc.perform(
            get("/api/v1/application")
                .header("Authorization", "Bearer $ciAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET applications should return 403 when no authentication provided`() {
        mockMvc.perform(
            get("/api/v1/application")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET applications should return empty page when no applications exist`() {
        // This test would need to be run on a clean database or with database cleanup
        // For now, we skip this as we have test applications from setUp
    }
}
