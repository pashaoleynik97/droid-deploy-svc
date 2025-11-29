package com.pashaoleynik97.droiddeploy.integration

import com.fasterxml.jackson.databind.ObjectMapper
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant
import java.util.UUID

class UserActivateIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private val objectMapper = ObjectMapper()

    private lateinit var superAdminUser: User
    private lateinit var adminUser: User
    private lateinit var targetAdminUser: User
    private lateinit var ciUser: User

    private lateinit var superAdminAccessToken: String
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

        // Load super admin user (created by SuperAdminInitializer)
        superAdminUser = userRepository.findByLogin("super_admin_test")
            ?: throw IllegalStateException("Super admin user should be created by SuperAdminInitializer")

        // Create regular ADMIN user
        adminUser = User(
            id = UUID.randomUUID(),
            login = "admin_activate_$uniqueId",
            passwordHash = passwordEncoder.encode("AdminPass1!"),
            role = UserRole.ADMIN,
            isActive = true,
            tokenVersion = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastLoginAt = null,
            lastInteractionAt = null
        )
        userRepository.save(adminUser)

        // Create target ADMIN user (to be activated/deactivated)
        targetAdminUser = User(
            id = UUID.randomUUID(),
            login = "target_admin_$uniqueId",
            passwordHash = passwordEncoder.encode("TargetAdminPass1!"),
            role = UserRole.ADMIN,
            isActive = true,
            tokenVersion = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastLoginAt = null,
            lastInteractionAt = null
        )
        userRepository.save(targetAdminUser)

        // Create CI user
        ciUser = User(
            id = UUID.randomUUID(),
            login = "ci_activate_$uniqueId",
            passwordHash = passwordEncoder.encode("CiPass1!"),
            role = UserRole.CI,
            isActive = true,
            tokenVersion = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastLoginAt = null,
            lastInteractionAt = null
        )
        userRepository.save(ciUser)

        // Generate access tokens
        superAdminAccessToken = jwtTokenProvider.createAccessToken(superAdminUser.id, superAdminUser.role, superAdminUser.tokenVersion)
        adminAccessToken = jwtTokenProvider.createAccessToken(adminUser.id, adminUser.role, adminUser.tokenVersion)
        ciAccessToken = jwtTokenProvider.createAccessToken(ciUser.id, ciUser.role, ciUser.tokenVersion)
    }

    @Test
    fun `PUT activate should return 200 when admin deactivates another user`() {
        val requestBody = mapOf("setActive" to false)

        mockMvc.perform(
            put("/api/v1/user/${targetAdminUser.id}/activate")
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("User deactivated successfully"))
            .andExpect(jsonPath("$.errors").isEmpty)

        // Verify user is deactivated
        val updatedUser = userRepository.findById(targetAdminUser.id)!!
        assert(!updatedUser.isActive)

        // Verify tokenVersion was incremented
        assert(updatedUser.tokenVersion == 1)

        // Verify updatedAt was changed
        assert(updatedUser.updatedAt.isAfter(targetAdminUser.updatedAt))
    }

    @Test
    fun `PUT activate should return 200 when admin activates a deactivated user`() {
        // First deactivate the user
        val deactivatedUser = targetAdminUser.copy(
            isActive = false,
            tokenVersion = 5,
            updatedAt = Instant.now()
        )
        userRepository.save(deactivatedUser)

        val requestBody = mapOf("setActive" to true)

        mockMvc.perform(
            put("/api/v1/user/${targetAdminUser.id}/activate")
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("User activated successfully"))
            .andExpect(jsonPath("$.errors").isEmpty)

        // Verify user is activated
        val updatedUser = userRepository.findById(targetAdminUser.id)!!
        assert(updatedUser.isActive)

        // Verify tokenVersion was incremented from 5 to 6
        assert(updatedUser.tokenVersion == 6)
    }

    @Test
    fun `PUT activate should return 403 when admin tries to modify their own status`() {
        val requestBody = mapOf("setActive" to false)

        mockMvc.perform(
            put("/api/v1/user/${adminUser.id}/activate")
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("AUTHORIZATION"))
            .andExpect(jsonPath("$.errors[0].message").value("You cannot change your own active status"))

        // Verify user status unchanged
        val unchangedUser = userRepository.findById(adminUser.id)!!
        assert(unchangedUser.isActive)
        assert(unchangedUser.tokenVersion == 0)
    }

    @Test
    fun `PUT activate should return 403 when trying to modify super admin status`() {
        val requestBody = mapOf("setActive" to false)

        mockMvc.perform(
            put("/api/v1/user/${superAdminUser.id}/activate")
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("AUTHORIZATION"))
            .andExpect(jsonPath("$.errors[0].message").value("Super admin account cannot be modified"))

        // Verify super admin status unchanged
        val unchangedUser = userRepository.findById(superAdminUser.id)!!
        assert(unchangedUser.isActive)
    }

    @Test
    fun `PUT activate should return 403 even when super admin tries to modify their own status`() {
        val requestBody = mapOf("setActive" to false)

        mockMvc.perform(
            put("/api/v1/user/${superAdminUser.id}/activate")
                .header("Authorization", "Bearer $superAdminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("AUTHORIZATION"))
            .andExpect(jsonPath("$.errors[0].message").value("You cannot change your own active status"))
    }

    @Test
    fun `PUT activate should return 404 when user does not exist`() {
        val requestBody = mapOf("setActive" to false)
        val nonExistentUserId = UUID.randomUUID()

        mockMvc.perform(
            put("/api/v1/user/$nonExistentUserId/activate")
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("NOT_FOUND"))
    }

    @Test
    fun `PUT activate should return 403 when CI user tries to activate a user`() {
        val requestBody = mapOf("setActive" to true)

        mockMvc.perform(
            put("/api/v1/user/${targetAdminUser.id}/activate")
                .header("Authorization", "Bearer $ciAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("AUTHORIZATION"))
    }

    @Test
    fun `PUT activate should return 403 when no token provided`() {
        val requestBody = mapOf("setActive" to false)

        mockMvc.perform(
            put("/api/v1/user/${targetAdminUser.id}/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `PUT activate should work for CI users being deactivated by admin`() {
        val requestBody = mapOf("setActive" to false)

        mockMvc.perform(
            put("/api/v1/user/${ciUser.id}/activate")
                .header("Authorization", "Bearer $adminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("User deactivated successfully"))

        // Verify CI user is deactivated
        val updatedUser = userRepository.findById(ciUser.id)!!
        assert(!updatedUser.isActive)
        assert(updatedUser.tokenVersion == 1)
    }
}
