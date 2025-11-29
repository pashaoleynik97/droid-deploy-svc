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

class PasswordUpdateIntegrationTest : AbstractIntegrationTest() {

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
    private lateinit var regularAdminUser: User
    private lateinit var anotherAdminUser: User
    private lateinit var ciUser: User
    private lateinit var consumerUser: User

    private lateinit var superAdminAccessToken: String
    private lateinit var regularAdminAccessToken: String
    private lateinit var ciAccessToken: String

    @BeforeEach
    fun setUp() {
        // Build MockMvc with Spring Security
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()

        // Create unique test logins to avoid conflicts
        val uniqueId = System.currentTimeMillis()

        // Load super admin user (created by SuperAdminInitializer with login from application-test.yaml)
        superAdminUser = userRepository.findByLogin("super_admin_test")
            ?: throw IllegalStateException("Super admin user should be created by SuperAdminInitializer")

        // Create regular ADMIN user
        regularAdminUser = User(
            id = UUID.randomUUID(),
            login = "regular_admin_$uniqueId",
            passwordHash = passwordEncoder.encode("RegularAdminPass1!"),
            role = UserRole.ADMIN,
            isActive = true,
            tokenVersion = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastLoginAt = null,
            lastInteractionAt = null
        )
        userRepository.save(regularAdminUser)

        // Create another ADMIN user
        anotherAdminUser = User(
            id = UUID.randomUUID(),
            login = "another_admin_$uniqueId",
            passwordHash = passwordEncoder.encode("AnotherAdminPass1!"),
            role = UserRole.ADMIN,
            isActive = true,
            tokenVersion = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastLoginAt = null,
            lastInteractionAt = null
        )
        userRepository.save(anotherAdminUser)

        // Create CI user
        ciUser = User(
            id = UUID.randomUUID(),
            login = "ci_user_$uniqueId",
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

        // Create CONSUMER user
        consumerUser = User(
            id = UUID.randomUUID(),
            login = "consumer_user_$uniqueId",
            passwordHash = passwordEncoder.encode("ConsumerPass1!"),
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
        superAdminAccessToken = jwtTokenProvider.createAccessToken(superAdminUser.id, superAdminUser.role, superAdminUser.tokenVersion)
        regularAdminAccessToken = jwtTokenProvider.createAccessToken(regularAdminUser.id, regularAdminUser.role, regularAdminUser.tokenVersion)
        ciAccessToken = jwtTokenProvider.createAccessToken(ciUser.id, ciUser.role, ciUser.tokenVersion)
    }

    @Test
    fun `PUT password should return 200 when admin updates their own password`() {
        val requestBody = mapOf("newPassword" to "NewPassword123!")

        mockMvc.perform(
            put("/api/v1/user/${regularAdminUser.id}/password")
                .header("Authorization", "Bearer $regularAdminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Password updated successfully"))
            .andExpect(jsonPath("$.errors").isEmpty)

        // Verify tokenVersion was incremented
        val updatedUser = userRepository.findById(regularAdminUser.id)!!
        assert(updatedUser.tokenVersion == 1)

        // Verify updatedAt was changed
        assert(updatedUser.updatedAt.isAfter(regularAdminUser.updatedAt))

        // Verify password was changed
        assert(passwordEncoder.matches("NewPassword123!", updatedUser.passwordHash))
    }

    @Test
    fun `PUT password should return 200 when super admin updates another admin's password`() {
        val requestBody = mapOf("newPassword" to "NewPassword456!")

        mockMvc.perform(
            put("/api/v1/user/${anotherAdminUser.id}/password")
                .header("Authorization", "Bearer $superAdminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Password updated successfully"))

        // Verify tokenVersion was incremented
        val updatedUser = userRepository.findById(anotherAdminUser.id)!!
        assert(updatedUser.tokenVersion == 1)
    }

    @Test
    fun `PUT password should return 403 when regular admin tries to update another user's password`() {
        val requestBody = mapOf("newPassword" to "NewPassword789!")

        mockMvc.perform(
            put("/api/v1/user/${anotherAdminUser.id}/password")
                .header("Authorization", "Bearer $regularAdminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("AUTHORIZATION"))
            .andExpect(jsonPath("$.errors[0].message").value("You can only update your own password"))
    }

    @Test
    fun `PUT password should return 400 when trying to update password for CI user`() {
        val requestBody = mapOf("newPassword" to "NewPassword999!")

        mockMvc.perform(
            put("/api/v1/user/${ciUser.id}/password")
                .header("Authorization", "Bearer $superAdminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("VALIDATION"))
            .andExpect(jsonPath("$.errors[0].message").value("User with role CI cannot have password updated. Only ADMIN users can have passwords."))
    }

    @Test
    fun `PUT password should return 400 when trying to update password for CONSUMER user`() {
        val requestBody = mapOf("newPassword" to "NewPassword000!")

        mockMvc.perform(
            put("/api/v1/user/${consumerUser.id}/password")
                .header("Authorization", "Bearer $superAdminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("VALIDATION"))
            .andExpect(jsonPath("$.errors[0].message").value("User with role CONSUMER cannot have password updated. Only ADMIN users can have passwords."))
    }

    @Test
    fun `PUT password should return 400 when password is too short`() {
        val requestBody = mapOf("newPassword" to "Short1!")

        mockMvc.perform(
            put("/api/v1/user/${regularAdminUser.id}/password")
                .header("Authorization", "Bearer $regularAdminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("VALIDATION"))
    }

    @Test
    fun `PUT password should return 400 when password missing uppercase`() {
        val requestBody = mapOf("newPassword" to "lowercase123!")

        mockMvc.perform(
            put("/api/v1/user/${regularAdminUser.id}/password")
                .header("Authorization", "Bearer $regularAdminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("VALIDATION"))
    }

    @Test
    fun `PUT password should return 400 when password missing lowercase`() {
        val requestBody = mapOf("newPassword" to "UPPERCASE123!")

        mockMvc.perform(
            put("/api/v1/user/${regularAdminUser.id}/password")
                .header("Authorization", "Bearer $regularAdminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("VALIDATION"))
    }

    @Test
    fun `PUT password should return 400 when password missing digit`() {
        val requestBody = mapOf("newPassword" to "NoDigitsHere!")

        mockMvc.perform(
            put("/api/v1/user/${regularAdminUser.id}/password")
                .header("Authorization", "Bearer $regularAdminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("VALIDATION"))
    }

    @Test
    fun `PUT password should return 404 when user does not exist`() {
        val requestBody = mapOf("newPassword" to "NewPassword123!")
        val nonExistentUserId = UUID.randomUUID()

        mockMvc.perform(
            put("/api/v1/user/$nonExistentUserId/password")
                .header("Authorization", "Bearer $superAdminAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("NOT_FOUND"))
    }

    @Test
    fun `PUT password should return 403 when CI user tries to update password`() {
        val requestBody = mapOf("newPassword" to "NewPassword123!")

        mockMvc.perform(
            put("/api/v1/user/${ciUser.id}/password")
                .header("Authorization", "Bearer $ciAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0].type").value("AUTHORIZATION"))
    }

    @Test
    fun `PUT password should return 403 when no token provided`() {
        val requestBody = mapOf("newPassword" to "NewPassword123!")

        mockMvc.perform(
            put("/api/v1/user/${regularAdminUser.id}/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isForbidden)
    }
}
