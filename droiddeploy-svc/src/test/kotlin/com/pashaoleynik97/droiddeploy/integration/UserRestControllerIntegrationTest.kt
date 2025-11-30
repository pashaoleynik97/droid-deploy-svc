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
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant
import java.util.UUID

class UserRestControllerIntegrationTest : AbstractIntegrationTest() {

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
    private lateinit var ciUser: User
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
            login = "admin_rest_$uniqueId",
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
            login = "ci_rest_$uniqueId",
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

        // Generate access tokens for both users
        adminAccessToken = jwtTokenProvider.createAccessToken(adminUser.id, adminUser.role, adminUser.tokenVersion)
        ciAccessToken = jwtTokenProvider.createAccessToken(ciUser.id, ciUser.role, ciUser.tokenVersion)
    }

    @Test
    fun `GET user by ID should return 200 when ADMIN accesses their own user`() {
        val result = mockMvc.perform(
            get("/api/v1/user/{userId}", adminUser.id)
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andReturn()

        println("Response body: ${result.response.contentAsString}")

        mockMvc.perform(
            get("/api/v1/user/{userId}", adminUser.id)
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(adminUser.id.toString()))
            .andExpect(jsonPath("$.data.login").value(adminUser.login))
            .andExpect(jsonPath("$.data.role").value("ADMIN"))
            .andExpect(jsonPath("$.data.isActive").value(true))
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
    }

    @Test
    fun `GET user by ID should return 200 when ADMIN accesses another user`() {
        mockMvc.perform(
            get("/api/v1/user/{userId}", ciUser.id)
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(ciUser.id.toString()))
            .andExpect(jsonPath("$.data.login").value(ciUser.login))
            .andExpect(jsonPath("$.data.role").value("CI"))
            .andExpect(jsonPath("$.data.isActive").value(true))
    }

    @Test
    fun `GET user by ID should return 200 when CI user accesses their own user`() {
        mockMvc.perform(
            get("/api/v1/user/{userId}", ciUser.id)
                .header("Authorization", "Bearer $ciAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(ciUser.id.toString()))
            .andExpect(jsonPath("$.data.login").value(ciUser.login))
            .andExpect(jsonPath("$.data.role").value("CI"))
    }

    @Test
    fun `GET user by ID should return 403 when CI user tries to access another user`() {
        mockMvc.perform(
            get("/api/v1/user/{userId}", adminUser.id)
                .header("Authorization", "Bearer $ciAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("AUTHORIZATION"))
            .andExpect(jsonPath("$.errors[0].message").value("You can only access your own user data"))
    }

    @Test
    fun `GET user by ID should return 404 when user does not exist`() {
        val nonExistentUserId = UUID.randomUUID()

        mockMvc.perform(
            get("/api/v1/user/{userId}", nonExistentUserId)
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("NOT_FOUND"))
            .andExpect(jsonPath("$.errors[0].message").value("User with ID '$nonExistentUserId' not found"))
    }

    @Test
    fun `GET user by ID should return 403 when no authentication token is provided`() {
        mockMvc.perform(
            get("/api/v1/user/{userId}", adminUser.id)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET user by ID should return 403 when invalid token is provided`() {
        mockMvc.perform(
            get("/api/v1/user/{userId}", adminUser.id)
                .header("Authorization", "Bearer invalid_token_here")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET user by ID should return 403 when refresh token is used instead of access token`() {
        val refreshToken = jwtTokenProvider.createRefreshToken(adminUser.id, adminUser.role, adminUser.tokenVersion)

        mockMvc.perform(
            get("/api/v1/user/{userId}", adminUser.id)
                .header("Authorization", "Bearer $refreshToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET user by ID should return user without sensitive fields`() {
        mockMvc.perform(
            get("/api/v1/user/{userId}", adminUser.id)
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.login").exists())
            .andExpect(jsonPath("$.data.role").exists())
            .andExpect(jsonPath("$.data.isActive").exists())
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.updatedAt").exists())
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.data.tokenVersion").doesNotExist())
    }

    @Test
    fun `GET user by ID should include optional timestamp fields when present`() {
        // Update user with lastLoginAt and lastInteractionAt
        val updatedUser = adminUser.copy(
            lastLoginAt = Instant.now().minusSeconds(3600),
            lastInteractionAt = Instant.now().minusSeconds(1800)
        )
        userRepository.save(updatedUser)

        mockMvc.perform(
            get("/api/v1/user/{userId}", adminUser.id)
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.lastLoginAt").exists())
            .andExpect(jsonPath("$.data.lastInteractionAt").exists())
    }
}
