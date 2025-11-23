package com.pashaoleynik97.droiddeploy.service.auth

import com.pashaoleynik97.droiddeploy.core.domain.User
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.core.exception.InvalidCredentialsException
import com.pashaoleynik97.droiddeploy.core.exception.UnauthorizedAccessException
import com.pashaoleynik97.droiddeploy.core.exception.UserNotActiveException
import com.pashaoleynik97.droiddeploy.core.repository.UserRepository
import com.pashaoleynik97.droiddeploy.rest.model.auth.LoginRequestDto
import com.pashaoleynik97.droiddeploy.security.JwtTokenProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.*

class AuthServiceImplTest {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var authService: AuthServiceImpl

    @BeforeEach
    fun setUp() {
        userRepository = mock()
        passwordEncoder = mock()
        jwtTokenProvider = mock()
        authService = AuthServiceImpl(userRepository, passwordEncoder, jwtTokenProvider)
    }

    @Test
    fun `login should return token pair for valid ADMIN credentials`() {
        // Given
        val request = LoginRequestDto(login = "admin_user", password = "correct_password")
        val user = createTestUser(login = "admin_user", role = UserRole.ADMIN, isActive = true)
        val tokenPair = createTestTokenPair()

        whenever(userRepository.findByLogin(request.login)).thenReturn(user)
        whenever(passwordEncoder.matches(request.password, user.passwordHash!!)).thenReturn(true)
        whenever(userRepository.save(any())).thenAnswer { invocation -> invocation.getArgument(0) }
        whenever(jwtTokenProvider.generateTokenPairForAdmin(any())).thenReturn(tokenPair)

        // When
        val result = authService.login(request)

        // Then
        assertNotNull(result)
        assertEquals(tokenPair.accessToken, result.accessToken)
        assertEquals(tokenPair.refreshToken, result.refreshToken)
        assertEquals(tokenPair.accessTokenExpiresAt.epochSecond, result.accessTokenExpiresAt)
        assertEquals(tokenPair.refreshTokenExpiresAt.epochSecond, result.refreshTokenExpiresAt)

        verify(userRepository).findByLogin(request.login)
        verify(passwordEncoder).matches(request.password, user.passwordHash!!)
        verify(userRepository).save(argThat { lastLoginAt != null })
        verify(jwtTokenProvider).generateTokenPairForAdmin(any())
    }

    @Test
    fun `login should update lastLoginAt timestamp`() {
        // Given
        val request = LoginRequestDto(login = "admin_user", password = "correct_password")
        val user = createTestUser(login = "admin_user", role = UserRole.ADMIN, lastLoginAt = null)
        val tokenPair = createTestTokenPair()

        whenever(userRepository.findByLogin(request.login)).thenReturn(user)
        whenever(passwordEncoder.matches(request.password, user.passwordHash!!)).thenReturn(true)
        whenever(userRepository.save(any())).thenAnswer { invocation -> invocation.getArgument(0) }
        whenever(jwtTokenProvider.generateTokenPairForAdmin(any())).thenReturn(tokenPair)

        // When
        authService.login(request)

        // Then
        verify(userRepository).save(argThat {
            lastLoginAt != null && lastLoginAt!!.isAfter(user.createdAt)
        })
    }

    @Test
    fun `login should throw InvalidCredentialsException when user not found`() {
        // Given
        val request = LoginRequestDto(login = "nonexistent_user", password = "password")

        whenever(userRepository.findByLogin(request.login)).thenReturn(null)

        // When & Then
        assertThrows<InvalidCredentialsException> {
            authService.login(request)
        }

        verify(userRepository).findByLogin(request.login)
        verify(passwordEncoder, never()).matches(any(), any())
        verify(userRepository, never()).save(any())
        verify(jwtTokenProvider, never()).generateTokenPairForAdmin(any())
    }

    @Test
    fun `login should throw InvalidCredentialsException when password is incorrect`() {
        // Given
        val request = LoginRequestDto(login = "admin_user", password = "wrong_password")
        val user = createTestUser(login = "admin_user", role = UserRole.ADMIN)

        whenever(userRepository.findByLogin(request.login)).thenReturn(user)
        whenever(passwordEncoder.matches(request.password, user.passwordHash!!)).thenReturn(false)

        // When & Then
        assertThrows<InvalidCredentialsException> {
            authService.login(request)
        }

        verify(userRepository).findByLogin(request.login)
        verify(passwordEncoder).matches(request.password, user.passwordHash!!)
        verify(userRepository, never()).save(any())
        verify(jwtTokenProvider, never()).generateTokenPairForAdmin(any())
    }

    @Test
    fun `login should throw InvalidCredentialsException when password hash is null`() {
        // Given
        val request = LoginRequestDto(login = "admin_user", password = "password")
        val user = createTestUser(login = "admin_user", role = UserRole.ADMIN, passwordHash = null)

        whenever(userRepository.findByLogin(request.login)).thenReturn(user)

        // When & Then
        assertThrows<InvalidCredentialsException> {
            authService.login(request)
        }

        verify(userRepository).findByLogin(request.login)
        verify(passwordEncoder, never()).matches(any(), any())
        verify(userRepository, never()).save(any())
        verify(jwtTokenProvider, never()).generateTokenPairForAdmin(any())
    }

    @Test
    fun `login should throw UnauthorizedAccessException when user is not ADMIN`() {
        // Given
        val request = LoginRequestDto(login = "ci_user", password = "correct_password")
        val user = createTestUser(login = "ci_user", role = UserRole.CI)

        whenever(userRepository.findByLogin(request.login)).thenReturn(user)

        // When & Then
        val exception = assertThrows<UnauthorizedAccessException> {
            authService.login(request)
        }

        assertEquals("Only ADMIN users can log in with username and password", exception.message)
        verify(userRepository).findByLogin(request.login)
        verify(passwordEncoder, never()).matches(any(), any())
        verify(userRepository, never()).save(any())
        verify(jwtTokenProvider, never()).generateTokenPairForAdmin(any())
    }

    @Test
    fun `login should throw UnauthorizedAccessException when user is CONSUMER`() {
        // Given
        val request = LoginRequestDto(login = "consumer_user", password = "correct_password")
        val user = createTestUser(login = "consumer_user", role = UserRole.CONSUMER)

        whenever(userRepository.findByLogin(request.login)).thenReturn(user)

        // When & Then
        val exception = assertThrows<UnauthorizedAccessException> {
            authService.login(request)
        }

        assertEquals("Only ADMIN users can log in with username and password", exception.message)
        verify(userRepository).findByLogin(request.login)
    }

    @Test
    fun `login should throw UserNotActiveException when user is inactive`() {
        // Given
        val request = LoginRequestDto(login = "inactive_admin", password = "correct_password")
        val user = createTestUser(login = "inactive_admin", role = UserRole.ADMIN, isActive = false)

        whenever(userRepository.findByLogin(request.login)).thenReturn(user)

        // When & Then
        assertThrows<UserNotActiveException> {
            authService.login(request)
        }

        verify(userRepository).findByLogin(request.login)
        verify(passwordEncoder, never()).matches(any(), any())
        verify(userRepository, never()).save(any())
        verify(jwtTokenProvider, never()).generateTokenPairForAdmin(any())
    }

    @Test
    fun `login should check role before checking if user is active`() {
        // Given
        val request = LoginRequestDto(login = "inactive_ci", password = "correct_password")
        val user = createTestUser(login = "inactive_ci", role = UserRole.CI, isActive = false)

        whenever(userRepository.findByLogin(request.login)).thenReturn(user)

        // When & Then
        // Should throw UnauthorizedAccessException (role check) not UserNotActiveException
        assertThrows<UnauthorizedAccessException> {
            authService.login(request)
        }

        verify(userRepository).findByLogin(request.login)
    }

    private fun createTestUser(
        id: UUID = UUID.randomUUID(),
        login: String = "test_user",
        passwordHash: String? = "hashedPassword",
        role: UserRole = UserRole.ADMIN,
        isActive: Boolean = true,
        lastLoginAt: Instant? = null
    ) = User(
        id = id,
        login = login,
        passwordHash = passwordHash,
        role = role,
        isActive = isActive,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        lastLoginAt = lastLoginAt,
        lastInteractionAt = null,
        tokenVersion = 0
    )

    private fun createTestTokenPair() = JwtTokenProvider.TokenPair(
        accessToken = "test.access.token",
        accessTokenExpiresAt = Instant.now().plusSeconds(900),
        refreshToken = "test.refresh.token",
        refreshTokenExpiresAt = Instant.now().plusSeconds(2592000)
    )
}
