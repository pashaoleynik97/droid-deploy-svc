package com.pashaoleynik97.droiddeploy.service.user

import com.pashaoleynik97.droiddeploy.core.domain.User
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.core.exception.InvalidLoginFormatException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidPasswordException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidRoleException
import com.pashaoleynik97.droiddeploy.core.exception.LoginAlreadyExistsException
import com.pashaoleynik97.droiddeploy.core.repository.UserRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.UUID

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
        Assertions.assertNotNull(result)
        Assertions.assertEquals(login, result.login)
        Assertions.assertEquals(encodedPassword, result.passwordHash)
        Assertions.assertEquals(role, result.role)
        Assertions.assertTrue(result.isActive)
        Assertions.assertEquals(0, result.tokenVersion)

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

        Assertions.assertTrue(exception.message!!.contains("CONSUMER"))
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

        Assertions.assertTrue(exception.message!!.contains(login))
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
        Assertions.assertNotNull(result)
        Assertions.assertEquals(login, result.login)
        Assertions.assertEquals(UserRole.CI, result.role)
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
        Assertions.assertNotNull(result)
        Assertions.assertEquals(login, result?.login)
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
        Assertions.assertNull(result)
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
        Assertions.assertTrue(result)
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
        Assertions.assertFalse(result)
        verify(userRepository).existsByLogin(login)
    }

    @Suppress("SameParameterValue")
    private fun createTestUser(login: String) = User(
        id = UUID.randomUUID(),
        login = login,
        passwordHash = "hashedPassword",
        role = UserRole.ADMIN,
        isActive = true,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        lastLoginAt = null,
        lastInteractionAt = null,
        tokenVersion = 0
    )
}