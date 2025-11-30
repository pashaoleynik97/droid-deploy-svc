package com.pashaoleynik97.droiddeploy.service.application

import com.pashaoleynik97.droiddeploy.core.domain.Application
import com.pashaoleynik97.droiddeploy.core.dto.application.CreateApplicationRequestDto
import com.pashaoleynik97.droiddeploy.core.exception.BundleIdAlreadyExistsException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidApplicationNameException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidBundleIdException
import com.pashaoleynik97.droiddeploy.core.repository.ApplicationRepository
import com.pashaoleynik97.droiddeploy.core.service.ApplicationService
import mu.KotlinLogging
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
}
