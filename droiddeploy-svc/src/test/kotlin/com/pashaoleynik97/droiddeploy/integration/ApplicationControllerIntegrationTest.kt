package com.pashaoleynik97.droiddeploy.integration

import com.pashaoleynik97.droiddeploy.AbstractIntegrationTest
import com.pashaoleynik97.droiddeploy.core.dto.application.CreateApplicationRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.application.UpdateApplicationRequestDto
import com.pashaoleynik97.droiddeploy.core.exception.ApplicationNotFoundException
import com.pashaoleynik97.droiddeploy.core.exception.BundleIdAlreadyExistsException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidApplicationNameException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidBundleIdException
import com.pashaoleynik97.droiddeploy.core.repository.ApplicationRepository
import com.pashaoleynik97.droiddeploy.core.service.ApplicationService
import com.pashaoleynik97.droiddeploy.db.entity.ApplicationEntity
import com.pashaoleynik97.droiddeploy.db.entity.ApplicationVersionEntity
import com.pashaoleynik97.droiddeploy.db.repository.JpaApplicationRepository
import com.pashaoleynik97.droiddeploy.db.repository.JpaApplicationVersionRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.UUID

class ApplicationControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var applicationService: ApplicationService

    @Autowired
    private lateinit var applicationRepository: ApplicationRepository

    @Autowired
    private lateinit var jpaApplicationRepository: JpaApplicationRepository

    @Autowired
    private lateinit var jpaApplicationVersionRepository: JpaApplicationVersionRepository

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

    @Test
    fun `updateApplication should update application with valid data`() {
        // Given
        val createRequest = CreateApplicationRequestDto(
            name = "Original App",
            bundleId = "com.example.original"
        )
        val createdApp = applicationService.createApplication(createRequest)

        val updateRequest = UpdateApplicationRequestDto(
            name = "Updated App Name",
            bundleId = "com.example.updated"
        )

        // When
        val updatedApp = applicationService.updateApplication(createdApp.id, updateRequest)

        // Then
        assertNotNull(updatedApp)
        assertEquals(createdApp.id, updatedApp.id)
        assertEquals("Updated App Name", updatedApp.name)
        assertEquals("com.example.updated", updatedApp.bundleId)
        assertEquals(createdApp.createdAt, updatedApp.createdAt)
    }

    @Test
    fun `updateApplication should allow updating only name without changing bundleId`() {
        // Given
        val createRequest = CreateApplicationRequestDto(
            name = "Original App",
            bundleId = "com.example.nameonly"
        )
        val createdApp = applicationService.createApplication(createRequest)

        val updateRequest = UpdateApplicationRequestDto(
            name = "New Name",
            bundleId = "com.example.nameonly"
        )

        // When
        val updatedApp = applicationService.updateApplication(createdApp.id, updateRequest)

        // Then
        assertEquals(createdApp.id, updatedApp.id)
        assertEquals("New Name", updatedApp.name)
        assertEquals("com.example.nameonly", updatedApp.bundleId)
    }

    @Test
    fun `updateApplication should throw ApplicationNotFoundException when application does not exist`() {
        // Given
        val nonExistentId = UUID.randomUUID()
        val updateRequest = UpdateApplicationRequestDto(
            name = "Test App",
            bundleId = "com.example.test"
        )

        // When & Then
        val exception = assertThrows<ApplicationNotFoundException> {
            applicationService.updateApplication(nonExistentId, updateRequest)
        }
        assertTrue(exception.message!!.contains(nonExistentId.toString()))
    }

    @Test
    fun `updateApplication should throw InvalidApplicationNameException when name is blank`() {
        // Given
        val createRequest = CreateApplicationRequestDto(
            name = "Original App",
            bundleId = "com.example.blankname2"
        )
        val createdApp = applicationService.createApplication(createRequest)

        val updateRequest = UpdateApplicationRequestDto(
            name = "  ",
            bundleId = "com.example.blankname2"
        )

        // When & Then
        assertThrows<InvalidApplicationNameException> {
            applicationService.updateApplication(createdApp.id, updateRequest)
        }
    }

    @Test
    fun `updateApplication should throw InvalidApplicationNameException when name exceeds maximum length`() {
        // Given
        val createRequest = CreateApplicationRequestDto(
            name = "Original App",
            bundleId = "com.example.longname2"
        )
        val createdApp = applicationService.createApplication(createRequest)

        val updateRequest = UpdateApplicationRequestDto(
            name = "a".repeat(256),
            bundleId = "com.example.longname2"
        )

        // When & Then
        assertThrows<InvalidApplicationNameException> {
            applicationService.updateApplication(createdApp.id, updateRequest)
        }
    }

    @Test
    fun `updateApplication should throw InvalidBundleIdException when bundleId is blank`() {
        // Given
        val createRequest = CreateApplicationRequestDto(
            name = "Original App",
            bundleId = "com.example.blankbundle"
        )
        val createdApp = applicationService.createApplication(createRequest)

        val updateRequest = UpdateApplicationRequestDto(
            name = "Updated App",
            bundleId = "  "
        )

        // When & Then
        assertThrows<InvalidBundleIdException> {
            applicationService.updateApplication(createdApp.id, updateRequest)
        }
    }

    @Test
    fun `updateApplication should throw InvalidBundleIdException when bundleId exceeds maximum length`() {
        // Given
        val createRequest = CreateApplicationRequestDto(
            name = "Original App",
            bundleId = "com.example.longbundle"
        )
        val createdApp = applicationService.createApplication(createRequest)

        val longBundleId = "com." + "a".repeat(252)
        val updateRequest = UpdateApplicationRequestDto(
            name = "Updated App",
            bundleId = longBundleId
        )

        // When & Then
        assertThrows<InvalidBundleIdException> {
            applicationService.updateApplication(createdApp.id, updateRequest)
        }
    }

    @Test
    fun `updateApplication should throw InvalidBundleIdException when bundleId has invalid format`() {
        // Given
        val createRequest = CreateApplicationRequestDto(
            name = "Original App",
            bundleId = "com.example.invalidformat"
        )
        val createdApp = applicationService.createApplication(createRequest)

        val updateRequest = UpdateApplicationRequestDto(
            name = "Updated App",
            bundleId = "com.Example.App"
        )

        // When & Then
        assertThrows<InvalidBundleIdException> {
            applicationService.updateApplication(createdApp.id, updateRequest)
        }
    }

    @Test
    fun `updateApplication should throw BundleIdAlreadyExistsException when new bundleId already exists`() {
        // Given
        val firstApp = applicationService.createApplication(
            CreateApplicationRequestDto(
                name = "First App",
                bundleId = "com.example.first2"
            )
        )

        val secondApp = applicationService.createApplication(
            CreateApplicationRequestDto(
                name = "Second App",
                bundleId = "com.example.second2"
            )
        )

        val updateRequest = UpdateApplicationRequestDto(
            name = "Updated Second App",
            bundleId = "com.example.first2"
        )

        // When & Then
        val exception = assertThrows<BundleIdAlreadyExistsException> {
            applicationService.updateApplication(secondApp.id, updateRequest)
        }
        assertTrue(exception.message!!.contains("com.example.first2"))
    }

    @Test
    fun `updateApplication should throw InvalidBundleIdException when changing bundleId with existing versions`() {
        // Given
        val createRequest = CreateApplicationRequestDto(
            name = "App With Versions",
            bundleId = "com.example.withversions"
        )
        val createdApp = applicationService.createApplication(createRequest)

        // Create a version for this application
        val applicationEntity = jpaApplicationRepository.findById(createdApp.id).orElseThrow()
        val versionEntity = ApplicationVersionEntity(
            id = UUID.randomUUID(),
            application = applicationEntity,
            versionName = "1.0.0",
            versionCode = 1,
            stable = true,
            createdAt = Instant.now()
        )
        jpaApplicationVersionRepository.save(versionEntity)

        val updateRequest = UpdateApplicationRequestDto(
            name = "Updated App With Versions",
            bundleId = "com.example.newbundleid"
        )

        // When & Then
        val exception = assertThrows<InvalidBundleIdException> {
            applicationService.updateApplication(createdApp.id, updateRequest)
        }
        assertTrue(exception.message!!.contains("Bundle id can not be changed when versions already exist"))
    }

    @Test
    fun `updateApplication should allow updating name when application has versions`() {
        // Given
        val createRequest = CreateApplicationRequestDto(
            name = "App With Versions 2",
            bundleId = "com.example.withversions2"
        )
        val createdApp = applicationService.createApplication(createRequest)

        // Create a version for this application
        val applicationEntity = jpaApplicationRepository.findById(createdApp.id).orElseThrow()
        val versionEntity = ApplicationVersionEntity(
            id = UUID.randomUUID(),
            application = applicationEntity,
            versionName = "1.0.0",
            versionCode = 1,
            stable = true,
            createdAt = Instant.now()
        )
        jpaApplicationVersionRepository.save(versionEntity)

        val updateRequest = UpdateApplicationRequestDto(
            name = "Updated Name Only",
            bundleId = "com.example.withversions2"
        )

        // When
        val updatedApp = applicationService.updateApplication(createdApp.id, updateRequest)

        // Then
        assertEquals(createdApp.id, updatedApp.id)
        assertEquals("Updated Name Only", updatedApp.name)
        assertEquals("com.example.withversions2", updatedApp.bundleId)
    }

    @Test
    fun `deleteApplication should delete existing application`() {
        // Given
        val createRequest = CreateApplicationRequestDto(
            name = "App To Delete",
            bundleId = "com.example.todelete"
        )
        val createdApp = applicationService.createApplication(createRequest)

        // Verify application exists
        val foundBeforeDelete = applicationRepository.findById(createdApp.id)
        assertNotNull(foundBeforeDelete)

        // When
        applicationService.deleteApplication(createdApp.id)

        // Then
        val foundAfterDelete = applicationRepository.findById(createdApp.id)
        assertNull(foundAfterDelete)
    }

    @Test
    fun `deleteApplication should delete application and its versions`() {
        // Given
        val createRequest = CreateApplicationRequestDto(
            name = "App With Versions To Delete",
            bundleId = "com.example.withversionstodelete"
        )
        val createdApp = applicationService.createApplication(createRequest)

        // Create versions for this application
        val applicationEntity = jpaApplicationRepository.findById(createdApp.id).orElseThrow()
        val version1 = ApplicationVersionEntity(
            id = UUID.randomUUID(),
            application = applicationEntity,
            versionName = "1.0.0",
            versionCode = 1,
            stable = true,
            createdAt = Instant.now()
        )
        val version2 = ApplicationVersionEntity(
            id = UUID.randomUUID(),
            application = applicationEntity,
            versionName = "2.0.0",
            versionCode = 2,
            stable = true,
            createdAt = Instant.now()
        )
        jpaApplicationVersionRepository.save(version1)
        jpaApplicationVersionRepository.save(version2)

        // Verify application and versions exist
        assertTrue(jpaApplicationRepository.existsById(createdApp.id))
        assertTrue(jpaApplicationVersionRepository.existsById(version1.id))
        assertTrue(jpaApplicationVersionRepository.existsById(version2.id))

        // When
        applicationService.deleteApplication(createdApp.id)

        // Then - Application and versions should be deleted
        assertFalse(jpaApplicationRepository.existsById(createdApp.id))
        assertFalse(jpaApplicationVersionRepository.existsById(version1.id))
        assertFalse(jpaApplicationVersionRepository.existsById(version2.id))
    }

    @Test
    fun `deleteApplication should throw ApplicationNotFoundException when application not found`() {
        // Given
        val nonExistentId = UUID.randomUUID()

        // When & Then
        val exception = assertThrows<ApplicationNotFoundException> {
            applicationService.deleteApplication(nonExistentId)
        }
        assertTrue(exception.message!!.contains(nonExistentId.toString()))
    }
}
