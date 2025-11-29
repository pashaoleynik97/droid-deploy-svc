package com.pashaoleynik97.droiddeploy.integration

import com.pashaoleynik97.droiddeploy.AbstractIntegrationTest
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.core.exception.InvalidLoginFormatException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidPasswordException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidRoleException
import com.pashaoleynik97.droiddeploy.core.exception.LoginAlreadyExistsException
import com.pashaoleynik97.droiddeploy.core.service.UserService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class UserControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var userService: UserService

    @Test
    fun `createUser should create user with valid ADMIN role`() {
        // Given
        val login = "newAdminUser"
        val password = "ValidPass123"
        val role = UserRole.ADMIN

        // When
        val user = userService.createUser(login, password, role)

        // Then
        assertNotNull(user)
        assertEquals(login, user.login)
        assertEquals(UserRole.ADMIN, user.role)
        assertTrue(user.isActive)
        assertNotNull(user.passwordHash)
        assertNull(user.lastLoginAt)
        assertNull(user.lastInteractionAt)
        assertEquals(0, user.tokenVersion)

        // Verify user exists in DB
        assertTrue(userService.userExists(login))
    }

    @Test
    fun `createUser should create user with valid CI role`() {
        // Given
        val login = "ciUser123"
        val password = "CiPassword1"
        val role = UserRole.CI

        // When
        val user = userService.createUser(login, password, role)

        // Then
        assertNotNull(user)
        assertEquals(login, user.login)
        assertEquals(UserRole.CI, user.role)
        assertTrue(user.isActive)
    }

    @Test
    fun `createUser should throw InvalidLoginFormatException when login is too short`() {
        // Given
        val login = "ab"  // Only 2 chars
        val password = "ValidPass123"
        val role = UserRole.ADMIN

        // When & Then
        assertThrows<InvalidLoginFormatException> {
            userService.createUser(login, password, role)
        }
    }

    @Test
    fun `createUser should throw InvalidLoginFormatException when login is too long`() {
        // Given
        val login = "a".repeat(21)  // 21 chars
        val password = "ValidPass123"
        val role = UserRole.ADMIN

        // When & Then
        assertThrows<InvalidLoginFormatException> {
            userService.createUser(login, password, role)
        }
    }

    @Test
    fun `createUser should throw InvalidLoginFormatException when login contains invalid characters`() {
        // Given
        val login = "user@test"  // @ not allowed
        val password = "ValidPass123"
        val role = UserRole.ADMIN

        // When & Then
        assertThrows<InvalidLoginFormatException> {
            userService.createUser(login, password, role)
        }
    }

    @Test
    fun `createUser should throw LoginAlreadyExistsException when login already exists (case-insensitive)`() {
        // Given - Create first user
        val existingLogin = "existingUser99"
        userService.createUser(existingLogin, "ValidPass123", UserRole.ADMIN)

        // Try to create with different case
        val login = "EXISTINGUSER99"  // Same login, different case
        val password = "ValidPass123"
        val role = UserRole.ADMIN

        // When & Then
        val exception = assertThrows<LoginAlreadyExistsException> {
            userService.createUser(login, password, role)
        }

        assertTrue(exception.message!!.contains(login))
    }

    @Test
    fun `createUser should throw InvalidPasswordException when password is too short`() {
        // Given
        val login = "validUser1"
        val password = "Short1"  // Less than 10 chars
        val role = UserRole.ADMIN

        // When & Then
        assertThrows<InvalidPasswordException> {
            userService.createUser(login, password, role)
        }
    }

    @Test
    fun `createUser should throw InvalidPasswordException when password lacks uppercase`() {
        // Given
        val login = "validUser2"
        val password = "lowercase123"
        val role = UserRole.ADMIN

        // When & Then
        assertThrows<InvalidPasswordException> {
            userService.createUser(login, password, role)
        }
    }

    @Test
    fun `createUser should throw InvalidPasswordException when password lacks lowercase`() {
        // Given
        val login = "validUser3"
        val password = "UPPERCASE123"
        val role = UserRole.ADMIN

        // When & Then
        assertThrows<InvalidPasswordException> {
            userService.createUser(login, password, role)
        }
    }

    @Test
    fun `createUser should throw InvalidPasswordException when password lacks digit`() {
        // Given
        val login = "validUser4"
        val password = "NoDigitsHere"
        val role = UserRole.ADMIN

        // When & Then
        assertThrows<InvalidPasswordException> {
            userService.createUser(login, password, role)
        }
    }

    @Test
    fun `createUser should throw InvalidRoleException when role is CONSUMER`() {
        // Given
        val login = "consumerUser"
        val password = "ValidPass123"
        val role = UserRole.CONSUMER

        // When & Then
        val exception = assertThrows<InvalidRoleException> {
            userService.createUser(login, password, role)
        }

        assertTrue(exception.message!!.contains("CONSUMER"))
    }

    @Test
    fun `createUser should accept login with underscores and dashes`() {
        // Given
        val login = "valid_user-123"
        val password = "ValidPass123"
        val role = UserRole.ADMIN

        // When
        val user = userService.createUser(login, password, role)

        // Then
        assertNotNull(user)
        assertEquals(login, user.login)
        assertTrue(userService.userExists(login))
    }
}
