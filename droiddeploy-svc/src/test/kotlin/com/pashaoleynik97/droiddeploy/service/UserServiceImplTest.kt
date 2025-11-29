package com.pashaoleynik97.droiddeploy.service

import com.pashaoleynik97.droiddeploy.core.domain.User
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.core.exception.InvalidLoginFormatException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidPasswordException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidRoleException
import com.pashaoleynik97.droiddeploy.core.exception.LoginAlreadyExistsException
import com.pashaoleynik97.droiddeploy.core.repository.UserRepository
import com.pashaoleynik97.droiddeploy.service.user.UserServiceImpl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.security.crypto.password.PasswordEncoder

class UserServiceImplTest {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var userService: UserServiceImpl

    @BeforeEach
    fun setUp() {
        userRepository = mock()
        passwordEncoder = mock()
        userService = UserServiceImpl(userRepository, passwordEncoder)
    }

    @Test
    fun `createUser should create user with valid inputs`() {
        // Given
        val login = "testUser123"
        val password = "ValidPass123"
        val role = UserRole.ADMIN
        val encodedPassword = "encodedPassword"

        whenever(userRepository.existsByLoginIgnoreCase(login)).thenReturn(false)
        whenever(passwordEncoder.encode(password)).thenReturn(encodedPassword)
        whenever(userRepository.save(any())).thenAnswer { invocation ->
            invocation.getArgument<User>(0)
        }

        // When
        val result = userService.createUser(login, password, role)

        // Then
        assertNotNull(result)
        assertEquals(login, result.login)
        assertEquals(encodedPassword, result.passwordHash)
        assertEquals(role, result.role)
        assertTrue(result.isActive)
        assertEquals(0, result.tokenVersion)

        verify(userRepository).existsByLoginIgnoreCase(login)
        verify(passwordEncoder).encode(password)
        verify(userRepository).save(any())
    }

    @Test
    fun `createUser should throw InvalidRoleException when role is CONSUMER`() {
        // Given
        val login = "testUser"
        val password = "ValidPass123"
        val role = UserRole.CONSUMER

        // When & Then
        val exception = assertThrows<InvalidRoleException> {
            userService.createUser(login, password, role)
        }

        assertTrue(exception.message!!.contains("CONSUMER"))
        verify(userRepository, never()).existsByLoginIgnoreCase(any())
        verify(passwordEncoder, never()).encode(any())
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `createUser should throw InvalidLoginFormatException when login is too short`() {
        // Given
        val login = "ab"  // Only 2 chars, min is 3
        val password = "ValidPass123"
        val role = UserRole.ADMIN

        // When & Then
        assertThrows<InvalidLoginFormatException> {
            userService.createUser(login, password, role)
        }

        verify(userRepository, never()).existsByLoginIgnoreCase(any())
    }

    @Test
    fun `createUser should throw InvalidLoginFormatException when login is too long`() {
        // Given
        val login = "a".repeat(21)  // 21 chars, max is 20
        val password = "ValidPass123"
        val role = UserRole.ADMIN

        // When & Then
        assertThrows<InvalidLoginFormatException> {
            userService.createUser(login, password, role)
        }

        verify(userRepository, never()).existsByLoginIgnoreCase(any())
    }

    @Test
    fun `createUser should throw InvalidLoginFormatException when login contains invalid characters`() {
        // Given
        val login = "test@user"  // @ is not allowed
        val password = "ValidPass123"
        val role = UserRole.ADMIN

        // When & Then
        assertThrows<InvalidLoginFormatException> {
            userService.createUser(login, password, role)
        }

        verify(userRepository, never()).existsByLoginIgnoreCase(any())
    }

    @Test
    fun `createUser should throw LoginAlreadyExistsException when login exists (case-insensitive)`() {
        // Given
        val login = "TestUser"
        val password = "ValidPass123"
        val role = UserRole.ADMIN

        whenever(userRepository.existsByLoginIgnoreCase(login)).thenReturn(true)

        // When & Then
        val exception = assertThrows<LoginAlreadyExistsException> {
            userService.createUser(login, password, role)
        }

        assertTrue(exception.message!!.contains(login))
        verify(userRepository).existsByLoginIgnoreCase(login)
        verify(passwordEncoder, never()).encode(any())
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `createUser should throw InvalidPasswordException when password is too short`() {
        // Given
        val login = "testUser"
        val password = "Short1"  // Less than 10 chars
        val role = UserRole.ADMIN

        whenever(userRepository.existsByLoginIgnoreCase(login)).thenReturn(false)

        // When & Then
        assertThrows<InvalidPasswordException> {
            userService.createUser(login, password, role)
        }

        verify(userRepository).existsByLoginIgnoreCase(login)
        verify(passwordEncoder, never()).encode(any())
    }

    @Test
    fun `createUser should throw InvalidPasswordException when password lacks uppercase`() {
        // Given
        val login = "testUser"
        val password = "lowercase123"  // No uppercase
        val role = UserRole.ADMIN

        whenever(userRepository.existsByLoginIgnoreCase(login)).thenReturn(false)

        // When & Then
        assertThrows<InvalidPasswordException> {
            userService.createUser(login, password, role)
        }

        verify(userRepository).existsByLoginIgnoreCase(login)
        verify(passwordEncoder, never()).encode(any())
    }

    @Test
    fun `createUser should throw InvalidPasswordException when password lacks lowercase`() {
        // Given
        val login = "testUser"
        val password = "UPPERCASE123"  // No lowercase
        val role = UserRole.ADMIN

        whenever(userRepository.existsByLoginIgnoreCase(login)).thenReturn(false)

        // When & Then
        assertThrows<InvalidPasswordException> {
            userService.createUser(login, password, role)
        }

        verify(userRepository).existsByLoginIgnoreCase(login)
        verify(passwordEncoder, never()).encode(any())
    }

    @Test
    fun `createUser should throw InvalidPasswordException when password lacks digit`() {
        // Given
        val login = "testUser"
        val password = "NoDigitsHere"  // No digit
        val role = UserRole.ADMIN

        whenever(userRepository.existsByLoginIgnoreCase(login)).thenReturn(false)

        // When & Then
        assertThrows<InvalidPasswordException> {
            userService.createUser(login, password, role)
        }

        verify(userRepository).existsByLoginIgnoreCase(login)
        verify(passwordEncoder, never()).encode(any())
    }

    @Test
    fun `createUser should accept login with underscores and dashes`() {
        // Given
        val login = "test_user-123"
        val password = "ValidPass123"
        val role = UserRole.CI

        whenever(userRepository.existsByLoginIgnoreCase(login)).thenReturn(false)
        whenever(passwordEncoder.encode(password)).thenReturn("encoded")
        whenever(userRepository.save(any())).thenAnswer { invocation ->
            invocation.getArgument<User>(0)
        }

        // When
        val result = userService.createUser(login, password, role)

        // Then
        assertNotNull(result)
        assertEquals(login, result.login)
        assertEquals(UserRole.CI, result.role)
    }

    @Test
    fun `findByLogin should return user when exists`() {
        // Given
        val login = "testuser"
        val user = createTestUser(login)

        whenever(userRepository.findByLogin(login)).thenReturn(user)

        // When
        val result = userService.findByLogin(login)

        // Then
        assertNotNull(result)
        assertEquals(login, result?.login)
        verify(userRepository).findByLogin(login)
    }

    @Test
    fun `findByLogin should return null when user does not exist`() {
        // Given
        val login = "nonexistent"

        whenever(userRepository.findByLogin(login)).thenReturn(null)

        // When
        val result = userService.findByLogin(login)

        // Then
        assertNull(result)
        verify(userRepository).findByLogin(login)
    }

    @Test
    fun `userExists should return true when user exists`() {
        // Given
        val login = "testuser"

        whenever(userRepository.existsByLogin(login)).thenReturn(true)

        // When
        val result = userService.userExists(login)

        // Then
        assertTrue(result)
        verify(userRepository).existsByLogin(login)
    }

    @Test
    fun `userExists should return false when user does not exist`() {
        // Given
        val login = "nonexistent"

        whenever(userRepository.existsByLogin(login)).thenReturn(false)

        // When
        val result = userService.userExists(login)

        // Then
        assertFalse(result)
        verify(userRepository).existsByLogin(login)
    }

    @Suppress("SameParameterValue")
    private fun createTestUser(login: String) = User(
        id = java.util.UUID.randomUUID(),
        login = login,
        passwordHash = "hashedPassword",
        role = UserRole.ADMIN,
        isActive = true,
        createdAt = java.time.Instant.now(),
        updatedAt = java.time.Instant.now(),
        lastLoginAt = null,
        lastInteractionAt = null,
        tokenVersion = 0
    )
}