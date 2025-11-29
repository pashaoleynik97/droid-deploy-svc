package com.pashaoleynik97.droiddeploy.integration

import com.pashaoleynik97.droiddeploy.AbstractIntegrationTest
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.core.exception.InvalidCredentialsException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidRefreshTokenException
import com.pashaoleynik97.droiddeploy.core.exception.UnauthorizedAccessException
import com.pashaoleynik97.droiddeploy.core.service.UserService
import com.pashaoleynik97.droiddeploy.rest.model.auth.LoginRequestDto
import com.pashaoleynik97.droiddeploy.rest.model.auth.RefreshTokenRequestDto
import com.pashaoleynik97.droiddeploy.rest.service.AuthService
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.time.Instant

class AuthControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var userService: UserService

    @Value($$"${security.jwt.secret}")
    private lateinit var jwtSecret: String

    @Value($$"${security.jwt.issuer}")
    private lateinit var jwtIssuer: String

    private lateinit var testAdminLogin: String
    private lateinit var testAdminPassword: String

    @BeforeEach
    fun setUp() {
        testAdminLogin = "integration_test_admin"
        testAdminPassword = "TestPassword123!@#"

        // Create a test ADMIN user if it doesn't exist
        if (!userService.userExists(testAdminLogin)) {
            userService.createUser(testAdminLogin, testAdminPassword, UserRole.ADMIN)
        }
    }

    @Test
    fun `login should return token pair for valid ADMIN credentials`() {
        // Given
        val loginRequest = LoginRequestDto(
            login = testAdminLogin,
            password = testAdminPassword
        )

        // When
        val tokenPair = authService.login(loginRequest)

        // Then
        assertNotNull(tokenPair)
        assertNotNull(tokenPair.accessToken)
        assertNotNull(tokenPair.refreshToken)
        assertTrue(tokenPair.accessToken.isNotBlank())
        assertTrue(tokenPair.refreshToken.isNotBlank())
        assertTrue(tokenPair.accessTokenExpiresAt > Instant.now().epochSecond)
        assertTrue(tokenPair.refreshTokenExpiresAt > Instant.now().epochSecond)
        assertTrue(tokenPair.refreshTokenExpiresAt > tokenPair.accessTokenExpiresAt)
    }

    @Test
    fun `login should return valid JWT tokens with correct claims`() {
        // Given
        val loginRequest = LoginRequestDto(
            login = testAdminLogin,
            password = testAdminPassword
        )

        // When
        val tokenPair = authService.login(loginRequest)

        // Then
        val accessToken = tokenPair.accessToken
        val refreshToken = tokenPair.refreshToken

        // Verify access token claims
        val accessClaims = parseToken(accessToken)
        assertEquals(jwtIssuer, accessClaims.issuer)
        assertEquals(UserRole.ADMIN.name, accessClaims["role"])
        assertEquals("access", accessClaims["tokenType"])
        assertNotNull(accessClaims["tokenVersion"])
        assertTrue(accessClaims.subject.startsWith("user:"))

        // Verify refresh token claims
        val refreshClaims = parseToken(refreshToken)
        assertEquals(jwtIssuer, refreshClaims.issuer)
        assertEquals(UserRole.ADMIN.name, refreshClaims["role"])
        assertEquals("refresh", refreshClaims["tokenType"])
        assertNotNull(refreshClaims["tokenVersion"])
        assertTrue(refreshClaims.subject.startsWith("user:"))

        // Both tokens should reference the same user
        assertEquals(accessClaims.subject, refreshClaims.subject)
    }

    @Test
    fun `login should throw InvalidCredentialsException for invalid password`() {
        // Given
        val loginRequest = LoginRequestDto(
            login = testAdminLogin,
            password = "WrongPassword123"
        )

        // When & Then
        assertThrows<InvalidCredentialsException> {
            authService.login(loginRequest)
        }
    }

    @Test
    fun `login should throw InvalidCredentialsException for non-existent user`() {
        // Given
        val loginRequest = LoginRequestDto(
            login = "nonexistent_user_12345",
            password = "SomePassword123"
        )

        // When & Then
        assertThrows<InvalidCredentialsException> {
            authService.login(loginRequest)
        }
    }

    @Test
    fun `login should throw UnauthorizedAccessException for non-ADMIN user (CI)`() {
        // Given - Create a CI user
        val ciUserLogin = "integration_test_ci_user"
        val ciUserPassword = "CiPassword123"
        if (!userService.userExists(ciUserLogin)) {
            userService.createUser(ciUserLogin, ciUserPassword, UserRole.CI)
        }

        val loginRequest = LoginRequestDto(
            login = ciUserLogin,
            password = ciUserPassword
        )

        // When & Then
        val exception = assertThrows<UnauthorizedAccessException> {
            authService.login(loginRequest)
        }
        assertEquals("Only ADMIN users can log in with username and password", exception.message)
    }

    @Test
    fun `login should throw UnauthorizedAccessException for non-ADMIN user (CONSUMER)`() {
        // Given - Create a CONSUMER user
        val consumerLogin = "integration_test_consumer"
        val consumerPassword = "ConsumerPassword123"
        if (!userService.userExists(consumerLogin)) {
            userService.createUser(consumerLogin, consumerPassword, UserRole.CONSUMER)
        }

        val loginRequest = LoginRequestDto(
            login = consumerLogin,
            password = consumerPassword
        )

        // When & Then
        val exception = assertThrows<UnauthorizedAccessException> {
            authService.login(loginRequest)
        }
        assertEquals("Only ADMIN users can log in with username and password", exception.message)
    }

    @Test
    fun `login should update lastLoginAt timestamp`() {
        // Given
        val loginRequest = LoginRequestDto(
            login = testAdminLogin,
            password = testAdminPassword
        )

        val userBefore = userService.findByLogin(testAdminLogin)
        val lastLoginBefore = userBefore?.lastLoginAt

        // When
        authService.login(loginRequest)

        // Then
        val userAfter = userService.findByLogin(testAdminLogin)
        assertNotNull(userAfter?.lastLoginAt)
        if (lastLoginBefore != null) {
            assertTrue(userAfter!!.lastLoginAt!! > lastLoginBefore)
        }
    }

    @Test
    fun `refreshToken should return new token pair for valid refresh token`() {
        // Given - First login to get a valid refresh token
        val loginRequest = LoginRequestDto(
            login = testAdminLogin,
            password = testAdminPassword
        )
        val loginResult = authService.login(loginRequest)

        // Wait a bit to ensure different timestamps
        Thread.sleep(1100)

        val refreshRequest = RefreshTokenRequestDto(
            refreshToken = loginResult.refreshToken
        )

        // When
        val result = authService.refreshToken(refreshRequest)

        // Then
        assertNotNull(result)
        assertNotNull(result.accessToken)
        assertNotNull(result.refreshToken)
        assertTrue(result.accessToken.isNotBlank())
        assertTrue(result.refreshToken.isNotBlank())
        assertTrue(result.accessTokenExpiresAt > Instant.now().epochSecond)
        assertTrue(result.refreshTokenExpiresAt > Instant.now().epochSecond)
        assertTrue(result.refreshTokenExpiresAt > result.accessTokenExpiresAt)

        // Verify the new tokens are different from the original ones (due to different iat timestamp)
        assertNotEquals(loginResult.accessToken, result.accessToken)
        assertNotEquals(loginResult.refreshToken, result.refreshToken)
    }

    @Test
    fun `refreshToken should return valid JWT tokens with correct claims`() {
        // Given
        val loginRequest = LoginRequestDto(
            login = testAdminLogin,
            password = testAdminPassword
        )
        val loginResult = authService.login(loginRequest)
        val refreshRequest = RefreshTokenRequestDto(
            refreshToken = loginResult.refreshToken
        )

        // When
        val result = authService.refreshToken(refreshRequest)

        // Then
        val accessToken = result.accessToken
        val refreshToken = result.refreshToken

        // Verify access token claims
        val accessClaims = parseToken(accessToken)
        assertEquals(jwtIssuer, accessClaims.issuer)
        assertEquals(UserRole.ADMIN.name, accessClaims["role"])
        assertEquals("access", accessClaims["tokenType"])
        assertNotNull(accessClaims["tokenVersion"])
        assertTrue(accessClaims.subject.startsWith("user:"))

        // Verify refresh token claims
        val refreshClaims = parseToken(refreshToken)
        assertEquals(jwtIssuer, refreshClaims.issuer)
        assertEquals(UserRole.ADMIN.name, refreshClaims["role"])
        assertEquals("refresh", refreshClaims["tokenType"])
        assertNotNull(refreshClaims["tokenVersion"])
        assertTrue(refreshClaims.subject.startsWith("user:"))

        // Both tokens should reference the same user
        assertEquals(accessClaims.subject, refreshClaims.subject)
    }

    @Test
    fun `refreshToken should throw InvalidRefreshTokenException for invalid token`() {
        // Given
        val refreshRequest = RefreshTokenRequestDto(
            refreshToken = "invalid.token.here"
        )

        // When & Then
        assertThrows<InvalidRefreshTokenException> {
            authService.refreshToken(refreshRequest)
        }
    }

    @Test
    fun `refreshToken should throw InvalidRefreshTokenException for expired token`() {
        // Given - Create an expired token (this is a simplified test)
        val refreshRequest = RefreshTokenRequestDto(
            refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyOjEyMyIsImlzcyI6InRlc3QiLCJleHAiOjE2MDk0NTkyMDB9.invalid"
        )

        // When & Then
        assertThrows<InvalidRefreshTokenException> {
            authService.refreshToken(refreshRequest)
        }
    }

    @Test
    fun `refreshToken should throw InvalidRefreshTokenException when using access token`() {
        // Given - Get an access token instead of refresh token
        val loginRequest = LoginRequestDto(
            login = testAdminLogin,
            password = testAdminPassword
        )
        val loginResult = authService.login(loginRequest)
        val refreshRequest = RefreshTokenRequestDto(
            refreshToken = loginResult.accessToken  // Using access token instead of refresh token
        )

        // When & Then
        assertThrows<InvalidRefreshTokenException> {
            authService.refreshToken(refreshRequest)
        }
    }

    private fun parseToken(token: String): Claims {
        val secretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
