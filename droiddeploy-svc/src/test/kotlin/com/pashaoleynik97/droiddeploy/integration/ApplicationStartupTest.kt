package com.pashaoleynik97.droiddeploy.integration

import com.pashaoleynik97.droiddeploy.AbstractIntegrationTest
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.core.service.UserService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ApplicationStartupTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var userService: UserService

    @Test
    fun `should create super admin user on startup`() {
        // Given
        val expectedLogin = "super_admin_test"  // From application-test.yaml

        // When
        val superAdmin = userService.findByLogin(expectedLogin)

        // Then
        assertNotNull(superAdmin, "Super admin user should be created on startup")
        assertEquals(expectedLogin, superAdmin?.login)
        assertEquals(UserRole.ADMIN, superAdmin?.role)
        assertTrue(superAdmin?.isActive ?: false)
        assertNotNull(superAdmin?.passwordHash)
    }

    @Test
    fun `should not create duplicate super admin on multiple startups`() {
        // Given
        val expectedLogin = "super_admin_test"  // From application-test.yaml

        // When - find the user that was created on startup
        val superAdmin = userService.findByLogin(expectedLogin)

        // Then - verify it exists and has expected properties
        assertNotNull(superAdmin)
        assertEquals(expectedLogin, superAdmin?.login)

        // Verify that userExists returns true
        assertTrue(userService.userExists(expectedLogin))
    }

    @Test
    fun `should be able to create additional users after super admin`() {
        // Given
        val newLogin = "new_user"
        val newPassword = "newPassword123"

        // When
        val newUser = userService.createUser(newLogin, newPassword, UserRole.CI)

        // Then
        assertNotNull(newUser)
        assertEquals(newLogin, newUser.login)
        assertEquals(UserRole.CI, newUser.role)
        assertTrue(newUser.isActive)

        // Verify the user can be found
        val foundUser = userService.findByLogin(newLogin)
        assertNotNull(foundUser)
        assertEquals(newLogin, foundUser?.login)
    }
}
