package com.pashaoleynik97.droiddeploy.integration

import com.pashaoleynik97.droiddeploy.AbstractIntegrationTest
import com.pashaoleynik97.droiddeploy.core.dto.application.CreateApplicationRequestDto
import com.pashaoleynik97.droiddeploy.core.exception.BundleIdAlreadyExistsException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidApplicationNameException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidBundleIdException
import com.pashaoleynik97.droiddeploy.core.repository.ApplicationRepository
import com.pashaoleynik97.droiddeploy.core.service.ApplicationService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ApplicationControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var applicationService: ApplicationService

    @Autowired
    private lateinit var applicationRepository: ApplicationRepository

    @Test
    fun `createApplication should create application with valid data`() {
        // Given
        val request = CreateApplicationRequestDto(
            name = "My Test App",
            bundleId = "com.example.testapp"
        )

        // When
        val application = applicationService.createApplication(request)

        // Then
        assertNotNull(application)
        assertEquals(request.name, application.name)
        assertEquals(request.bundleId, application.bundleId)
        assertNull(application.signingCertificateSha256)
        assertTrue(application.createdAt > 0)

        // Verify application exists in DB
        val found = applicationRepository.findByBundleId(request.bundleId)
        assertNotNull(found)
        assertEquals(application.id, found?.id)
    }

    @Test
    fun `createApplication should throw InvalidApplicationNameException when name is blank`() {
        // Given
        val request = CreateApplicationRequestDto(
            name = "  ",
            bundleId = "com.example.blankname"
        )

        // When & Then
        assertThrows<InvalidApplicationNameException> {
            applicationService.createApplication(request)
        }
    }

    @Test
    fun `createApplication should throw InvalidApplicationNameException when name exceeds maximum length`() {
        // Given
        val request = CreateApplicationRequestDto(
            name = "a".repeat(256),
            bundleId = "com.example.longname"
        )

        // When & Then
        assertThrows<InvalidApplicationNameException> {
            applicationService.createApplication(request)
        }
    }

    @Test
    fun `createApplication should throw InvalidBundleIdException when bundleId is blank`() {
        // Given
        val request = CreateApplicationRequestDto(
            name = "Valid App",
            bundleId = "  "
        )

        // When & Then
        assertThrows<InvalidBundleIdException> {
            applicationService.createApplication(request)
        }
    }

    @Test
    fun `createApplication should throw InvalidBundleIdException when bundleId exceeds maximum length`() {
        // Given
        val longBundleId = "com." + "a".repeat(252)
        val request = CreateApplicationRequestDto(
            name = "Valid App",
            bundleId = longBundleId
        )

        // When & Then
        assertThrows<InvalidBundleIdException> {
            applicationService.createApplication(request)
        }
    }

    @Test
    fun `createApplication should throw InvalidBundleIdException when bundleId has uppercase letters`() {
        // Given
        val request = CreateApplicationRequestDto(
            name = "Valid App",
            bundleId = "com.Example.App"
        )

        // When & Then
        assertThrows<InvalidBundleIdException> {
            applicationService.createApplication(request)
        }
    }

    @Test
    fun `createApplication should throw InvalidBundleIdException when bundleId has only one segment`() {
        // Given
        val request = CreateApplicationRequestDto(
            name = "Valid App",
            bundleId = "singlepart"
        )

        // When & Then
        assertThrows<InvalidBundleIdException> {
            applicationService.createApplication(request)
        }
    }

    @Test
    fun `createApplication should throw InvalidBundleIdException when bundleId segment starts with digit`() {
        // Given
        val request = CreateApplicationRequestDto(
            name = "Valid App",
            bundleId = "com.1example.app"
        )

        // When & Then
        assertThrows<InvalidBundleIdException> {
            applicationService.createApplication(request)
        }
    }

    @Test
    fun `createApplication should throw InvalidBundleIdException when bundleId contains invalid characters`() {
        // Given
        val request = CreateApplicationRequestDto(
            name = "Valid App",
            bundleId = "com.example.app-name"
        )

        // When & Then
        assertThrows<InvalidBundleIdException> {
            applicationService.createApplication(request)
        }
    }

    @Test
    fun `createApplication should accept bundleId with underscores and digits`() {
        // Given
        val request = CreateApplicationRequestDto(
            name = "Valid App",
            bundleId = "com.example.app_name123"
        )

        // When
        val application = applicationService.createApplication(request)

        // Then
        assertNotNull(application)
        assertEquals(request.bundleId, application.bundleId)
    }

    @Test
    fun `createApplication should accept bundleId with multiple segments`() {
        // Given
        val request = CreateApplicationRequestDto(
            name = "Valid App",
            bundleId = "com.example.sub.domain.app"
        )

        // When
        val application = applicationService.createApplication(request)

        // Then
        assertNotNull(application)
        assertEquals(request.bundleId, application.bundleId)
    }

    @Test
    fun `createApplication should throw BundleIdAlreadyExistsException when bundleId already exists`() {
        // Given
        val bundleId = "com.example.duplicate"
        val firstRequest = CreateApplicationRequestDto(
            name = "First App",
            bundleId = bundleId
        )
        applicationService.createApplication(firstRequest)

        val secondRequest = CreateApplicationRequestDto(
            name = "Second App",
            bundleId = bundleId
        )

        // When & Then
        val exception = assertThrows<BundleIdAlreadyExistsException> {
            applicationService.createApplication(secondRequest)
        }
        assertTrue(exception.message!!.contains(bundleId))
    }

    @Test
    fun `createApplication should allow different bundleIds for same application name`() {
        // Given
        val name = "Same Name App"
        val firstRequest = CreateApplicationRequestDto(
            name = name,
            bundleId = "com.example.first"
        )
        val secondRequest = CreateApplicationRequestDto(
            name = name,
            bundleId = "com.example.second"
        )

        // When
        val firstApp = applicationService.createApplication(firstRequest)
        val secondApp = applicationService.createApplication(secondRequest)

        // Then
        assertNotNull(firstApp)
        assertNotNull(secondApp)
        assertEquals(name, firstApp.name)
        assertEquals(name, secondApp.name)
        assertNotEquals(firstApp.bundleId, secondApp.bundleId)
        assertNotEquals(firstApp.id, secondApp.id)
    }
}
