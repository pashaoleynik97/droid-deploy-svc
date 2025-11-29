package com.pashaoleynik97.droiddeploy.integration

import com.pashaoleynik97.droiddeploy.AbstractIntegrationTest
import com.pashaoleynik97.droiddeploy.core.domain.User
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
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

class UserListIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    private lateinit var mockMvc: MockMvc

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
            login = "admin_list_$uniqueId",
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
            login = "ci_list_$uniqueId",
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

        // Create additional test users for filtering
        createTestUser("admin_active_$uniqueId", UserRole.ADMIN, true)
        createTestUser("admin_inactive_$uniqueId", UserRole.ADMIN, false)
        createTestUser("ci_active_$uniqueId", UserRole.CI, true)
        createTestUser("ci_inactive_$uniqueId", UserRole.CI, false)
        createTestUser("consumer_active_$uniqueId", UserRole.CONSUMER, true)

        // Generate access tokens
        adminAccessToken = jwtTokenProvider.createAccessToken(adminUser.id, adminUser.role, adminUser.tokenVersion)
        ciAccessToken = jwtTokenProvider.createAccessToken(ciUser.id, ciUser.role, ciUser.tokenVersion)
    }

    private fun createTestUser(login: String, role: UserRole, isActive: Boolean) {
        val user = User(
            id = UUID.randomUUID(),
            login = login,
            passwordHash = passwordEncoder.encode("TestPass123!"),
            role = role,
            isActive = isActive,
            tokenVersion = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastLoginAt = null,
            lastInteractionAt = null
        )
        userRepository.save(user)
    }

    @Test
    fun `GET users should return 200 with all users when no filters provided`() {
        mockMvc.perform(
            get("/api/v1/user")
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
    fun `GET users should filter by ADMIN role`() {
        mockMvc.perform(
            get("/api/v1/user")
                .param("role", "ADMIN")
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[*].role").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo("ADMIN"))))
    }

    @Test
    fun `GET users should filter by CI role`() {
        mockMvc.perform(
            get("/api/v1/user")
                .param("role", "CI")
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[*].role").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo("CI"))))
    }

    @Test
    fun `GET users should filter by CONSUMER role`() {
        mockMvc.perform(
            get("/api/v1/user")
                .param("role", "CONSUMER")
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[*].role").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo("CONSUMER"))))
    }

    @Test
    fun `GET users should filter by active status true`() {
        mockMvc.perform(
            get("/api/v1/user")
                .param("isActive", "true")
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[*].active").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo(true))))
    }

    @Test
    fun `GET users should filter by active status false`() {
        mockMvc.perform(
            get("/api/v1/user")
                .param("isActive", "false")
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[*].active").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo(false))))
    }

    @Test
    fun `GET users should filter by both role and active status`() {
        mockMvc.perform(
            get("/api/v1/user")
                .param("role", "ADMIN")
                .param("isActive", "true")
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[*].role").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo("ADMIN"))))
            .andExpect(jsonPath("$.data.content[*].active").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo(true))))
    }

    @Test
    fun `GET users should support custom page size`() {
        mockMvc.perform(
            get("/api/v1/user")
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
    fun `GET users should cap page size at 100`() {
        mockMvc.perform(
            get("/api/v1/user")
                .param("size", "200")
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.size").value(100))
    }

    @Test
    fun `GET users should support pagination`() {
        mockMvc.perform(
            get("/api/v1/user")
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
    fun `GET users should return 400 for invalid role`() {
        mockMvc.perform(
            get("/api/v1/user")
                .param("role", "INVALID_ROLE")
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET users should return 403 when accessed by non-ADMIN user`() {
        mockMvc.perform(
            get("/api/v1/user")
                .header("Authorization", "Bearer $ciAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET users should return 403 when no authentication provided`() {
        mockMvc.perform(
            get("/api/v1/user")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isForbidden)
    }
}
