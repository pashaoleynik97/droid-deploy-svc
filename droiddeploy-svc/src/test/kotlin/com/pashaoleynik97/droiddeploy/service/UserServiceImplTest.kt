package com.pashaoleynik97.droiddeploy.service

import com.pashaoleynik97.droiddeploy.core.domain.User
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.core.repository.UserRepository
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
    fun `createUser should create user when login does not exist`() {
        // Given
        val login = "testuser"
        val password = "password123"
        val role = UserRole.ADMIN
        val encodedPassword = "encodedPassword"

        whenever(userRepository.existsByLogin(login)).thenReturn(false)
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

        verify(userRepository).existsByLogin(login)
        verify(passwordEncoder).encode(password)
        verify(userRepository).save(any())
    }

    @Test
    fun `createUser should throw exception when user already exists`() {
        // Given
        val login = "existinguser"
        val password = "password123"
        val role = UserRole.ADMIN

        whenever(userRepository.existsByLogin(login)).thenReturn(true)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            userService.createUser(login, password, role)
        }

        assertEquals("User with login 'existinguser' already exists", exception.message)
        verify(userRepository).existsByLogin(login)
        verify(passwordEncoder, never()).encode(any())
        verify(userRepository, never()).save(any())
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