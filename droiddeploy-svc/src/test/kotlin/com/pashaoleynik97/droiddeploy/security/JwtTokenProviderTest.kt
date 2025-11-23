package com.pashaoleynik97.droiddeploy.security

import com.pashaoleynik97.droiddeploy.core.domain.User
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class JwtTokenProviderTest {

    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var jwtProperties: JwtProperties
    private val testSecret = "test-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm"

    @BeforeEach
    fun setUp() {
        jwtProperties = JwtProperties(
            secret = testSecret,
            issuer = "test-issuer",
            accessTokenValiditySeconds = 900L,  // 15 minutes
            refreshTokenValiditySeconds = 2592000L  // 30 days
        )
        jwtTokenProvider = JwtTokenProvider(jwtProperties)
    }

    @Test
    fun `generateTokenPairForAdmin should generate both access and refresh tokens`() {
        // Given
        val user = createTestUser()

        // When
        val tokenPair = jwtTokenProvider.generateTokenPairForAdmin(user)

        // Then
        assertNotNull(tokenPair.accessToken)
        assertNotNull(tokenPair.refreshToken)
        assertNotNull(tokenPair.accessTokenExpiresAt)
        assertNotNull(tokenPair.refreshTokenExpiresAt)
        assertTrue(tokenPair.accessToken.isNotBlank())
        assertTrue(tokenPair.refreshToken.isNotBlank())
    }

    @Test
    fun `generated access token should have correct claims`() {
        // Given
        val user = createTestUser()

        // When
        val tokenPair = jwtTokenProvider.generateTokenPairForAdmin(user)

        // Then
        val claims = parseToken(tokenPair.accessToken)
        assertEquals("user:${user.id}", claims.subject)
        assertEquals(jwtProperties.issuer, claims.issuer)
        assertEquals(UserRole.ADMIN.name, claims["role"])
        assertEquals("access", claims["tokenType"])
        assertEquals(user.tokenVersion, claims["tokenVersion"])
    }

    @Test
    fun `generated refresh token should have correct claims`() {
        // Given
        val user = createTestUser()

        // When
        val tokenPair = jwtTokenProvider.generateTokenPairForAdmin(user)

        // Then
        val claims = parseToken(tokenPair.refreshToken)
        assertEquals("user:${user.id}", claims.subject)
        assertEquals(jwtProperties.issuer, claims.issuer)
        assertEquals(UserRole.ADMIN.name, claims["role"])
        assertEquals("refresh", claims["tokenType"])
        assertEquals(user.tokenVersion, claims["tokenVersion"])
    }

    @Test
    fun `access token should expire after configured validity period`() {
        // Given
        val user = createTestUser()
        val now = Instant.now()

        // When
        val tokenPair = jwtTokenProvider.generateTokenPairForAdmin(user)

        // Then
        val expectedExpiry = now.plusSeconds(jwtProperties.accessTokenValiditySeconds)
        // Allow 5 seconds tolerance for test execution time
        assertTrue(tokenPair.accessTokenExpiresAt.isAfter(now))
        assertTrue(tokenPair.accessTokenExpiresAt.isBefore(expectedExpiry.plusSeconds(5)))
    }

    @Test
    fun `refresh token should expire after configured validity period`() {
        // Given
        val user = createTestUser()
        val now = Instant.now()

        // When
        val tokenPair = jwtTokenProvider.generateTokenPairForAdmin(user)

        // Then
        val expectedExpiry = now.plusSeconds(jwtProperties.refreshTokenValiditySeconds)
        // Allow 5 seconds tolerance for test execution time
        assertTrue(tokenPair.refreshTokenExpiresAt.isAfter(now))
        assertTrue(tokenPair.refreshTokenExpiresAt.isBefore(expectedExpiry.plusSeconds(5)))
    }

    @Test
    fun `access token should expire before refresh token`() {
        // Given
        val user = createTestUser()

        // When
        val tokenPair = jwtTokenProvider.generateTokenPairForAdmin(user)

        // Then
        assertTrue(tokenPair.accessTokenExpiresAt.isBefore(tokenPair.refreshTokenExpiresAt))
    }

    @Test
    fun `tokens should be different for same user`() {
        // Given
        val user = createTestUser()

        // When
        val tokenPair = jwtTokenProvider.generateTokenPairForAdmin(user)

        // Then
        assertNotEquals(tokenPair.accessToken, tokenPair.refreshToken)
    }

    @Test
    fun `tokens should include user token version`() {
        // Given
        val user = createTestUser(tokenVersion = 5)

        // When
        val tokenPair = jwtTokenProvider.generateTokenPairForAdmin(user)

        // Then
        val accessClaims = parseToken(tokenPair.accessToken)
        val refreshClaims = parseToken(tokenPair.refreshToken)
        assertEquals(5, accessClaims["tokenVersion"])
        assertEquals(5, refreshClaims["tokenVersion"])
    }

    private fun createTestUser(
        id: UUID = UUID.randomUUID(),
        login: String = "test_admin",
        role: UserRole = UserRole.ADMIN,
        tokenVersion: Int = 0
    ) = User(
        id = id,
        login = login,
        passwordHash = "hashedPassword",
        role = role,
        isActive = true,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        lastLoginAt = null,
        lastInteractionAt = null,
        tokenVersion = tokenVersion
    )

    private fun parseToken(token: String): Claims {
        val secretKey = Keys.hmacShaKeyFor(testSecret.toByteArray())
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
