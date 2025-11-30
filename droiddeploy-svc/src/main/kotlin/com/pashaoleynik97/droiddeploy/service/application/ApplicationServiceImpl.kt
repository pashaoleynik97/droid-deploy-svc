package com.pashaoleynik97.droiddeploy.service.application

import com.pashaoleynik97.droiddeploy.core.domain.Application
import com.pashaoleynik97.droiddeploy.core.dto.application.ApplicationResponseDto
import com.pashaoleynik97.droiddeploy.core.dto.application.CreateApplicationRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.application.UpdateApplicationRequestDto
import com.pashaoleynik97.droiddeploy.core.exception.ApplicationNotFoundException
import com.pashaoleynik97.droiddeploy.core.exception.BundleIdAlreadyExistsException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidApplicationNameException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidBundleIdException
import com.pashaoleynik97.droiddeploy.core.repository.ApplicationRepository
import com.pashaoleynik97.droiddeploy.core.service.ApplicationService
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class ApplicationServiceImpl(
    private val applicationRepository: ApplicationRepository
) : ApplicationService {

    companion object {
        private val BUNDLE_ID_REGEX = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")
        private const val MAX_NAME_LENGTH = 255
        private const val MAX_BUNDLE_ID_LENGTH = 255
    }

    override fun createApplication(request: CreateApplicationRequestDto): Application {
        logger.debug { "Attempting to create application with name: ${request.name}, bundleId: ${request.bundleId}" }

        // Validate name
        if (request.name.isBlank()) {
            logger.warn { "Failed to create application: name is blank" }
            throw InvalidApplicationNameException("Application name is invalid")
        }

        if (request.name.length > MAX_NAME_LENGTH) {
            logger.warn { "Failed to create application: name exceeds maximum length of $MAX_NAME_LENGTH" }
            throw InvalidApplicationNameException("Application name is invalid")
        }

        // Validate bundleId
        if (request.bundleId.isBlank()) {
            logger.warn { "Failed to create application: bundleId is blank" }
            throw InvalidBundleIdException("Bundle id is invalid")
        }

        if (request.bundleId.length > MAX_BUNDLE_ID_LENGTH) {
            logger.warn { "Failed to create application: bundleId exceeds maximum length of $MAX_BUNDLE_ID_LENGTH" }
            throw InvalidBundleIdException("Bundle id is invalid")
        }

        if (!BUNDLE_ID_REGEX.matches(request.bundleId)) {
            logger.warn { "Failed to create application: bundleId '${request.bundleId}' doesn't match required pattern" }
            throw InvalidBundleIdException("Bundle id is invalid")
        }

        // Check bundleId uniqueness
        if (applicationRepository.existsByBundleId(request.bundleId)) {
            logger.warn { "Failed to create application: bundleId '${request.bundleId}' already exists" }
            throw BundleIdAlreadyExistsException(request.bundleId)
        }

        val now = Instant.now()
        val application = Application(
            id = UUID.randomUUID(),
            name = request.name,
            bundleId = request.bundleId,
            signingCertificateSha256 = null,
            createdAt = now.toEpochMilli()
        )

        val savedApplication = applicationRepository.save(application)
        logger.info { "Application created successfully: name=${savedApplication.name}, id=${savedApplication.id}, bundleId=${savedApplication.bundleId}" }
        return savedApplication
    }

    override fun updateApplication(id: UUID, request: UpdateApplicationRequestDto): Application {
        logger.debug { "Attempting to update application with id: $id, name: ${request.name}, bundleId: ${request.bundleId}" }

        // Load application by id
        val application = applicationRepository.findById(id)
            ?: throw ApplicationNotFoundException(id)

        // Validate name
        if (request.name.isBlank()) {
            logger.warn { "Failed to update application: name is blank" }
            throw InvalidApplicationNameException("Application name is invalid")
        }

        if (request.name.length > MAX_NAME_LENGTH) {
            logger.warn { "Failed to update application: name exceeds maximum length of $MAX_NAME_LENGTH" }
            throw InvalidApplicationNameException("Application name is invalid")
        }

        // Validate bundleId format
        if (request.bundleId.isBlank()) {
            logger.warn { "Failed to update application: bundleId is blank" }
            throw InvalidBundleIdException("Bundle id is invalid")
        }

        if (request.bundleId.length > MAX_BUNDLE_ID_LENGTH) {
            logger.warn { "Failed to update application: bundleId exceeds maximum length of $MAX_BUNDLE_ID_LENGTH" }
            throw InvalidBundleIdException("Bundle id is invalid")
        }

        if (!BUNDLE_ID_REGEX.matches(request.bundleId)) {
            logger.warn { "Failed to update application: bundleId '${request.bundleId}' doesn't match required pattern" }
            throw InvalidBundleIdException("Bundle id is invalid")
        }

        // Check if bundleId is changing
        val bundleIdChanged = request.bundleId != application.bundleId

        if (bundleIdChanged) {
            // Check if application has versions
            if (applicationRepository.hasVersions(id)) {
                logger.warn { "Failed to update application: cannot change bundleId when versions already exist" }
                throw InvalidBundleIdException("Bundle id can not be changed when versions already exist")
            }

            // Check bundleId uniqueness
            val existingApp = applicationRepository.findByBundleId(request.bundleId)
            if (existingApp != null && existingApp.id != id) {
                logger.warn { "Failed to update application: bundleId '${request.bundleId}' already exists for another application" }
                throw BundleIdAlreadyExistsException(request.bundleId)
            }
        }

        // Update fields
        val updatedApplication = application.copy(
            name = request.name.trim(),
            bundleId = request.bundleId.trim()
        )

        val savedApplication = applicationRepository.save(updatedApplication)
        logger.info { "Application updated successfully: id=${savedApplication.id}, name=${savedApplication.name}, bundleId=${savedApplication.bundleId}" }
        return savedApplication
    }

    override fun listApplications(pageable: Pageable): Page<Application> {
        logger.debug { "Listing applications: page=${pageable.pageNumber}, size=${pageable.pageSize}" }
        val applications = applicationRepository.findAll(pageable)
        logger.info { "Retrieved ${applications.totalElements} applications, returning page ${applications.number} of ${applications.totalPages}" }
        return applications
    }

    override fun getApplicationById(id: UUID): ApplicationResponseDto {
        logger.debug { "Attempting to get application by id: $id" }

        val application = applicationRepository.findById(id)
            ?: throw ApplicationNotFoundException(id)

        logger.info { "Application found: id=${application.id}, name=${application.name}, bundleId=${application.bundleId}" }
        return ApplicationResponseDto.fromDomain(application)
    }
}
