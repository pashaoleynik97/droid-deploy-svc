package com.pashaoleynik97.droiddeploy.service.application

import com.pashaoleynik97.droiddeploy.core.domain.Application
import com.pashaoleynik97.droiddeploy.core.domain.ApplicationVersion
import com.pashaoleynik97.droiddeploy.core.dto.application.ApplicationResponseDto
import com.pashaoleynik97.droiddeploy.core.dto.application.CreateApplicationRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.application.UpdateApplicationRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.application.VersionDto
import com.pashaoleynik97.droiddeploy.core.exception.ApplicationNotFoundException
import com.pashaoleynik97.droiddeploy.core.exception.ApplicationVersionAlreadyExistsException
import com.pashaoleynik97.droiddeploy.core.exception.ApplicationVersionNotFoundException
import com.pashaoleynik97.droiddeploy.core.exception.ApkStorageException
import com.pashaoleynik97.droiddeploy.core.exception.BundleIdAlreadyExistsException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidApplicationNameException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidBundleIdException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidVersionCodeException
import com.pashaoleynik97.droiddeploy.core.exception.SigningCertificateMismatchException
import com.pashaoleynik97.droiddeploy.core.repository.ApplicationRepository
import com.pashaoleynik97.droiddeploy.core.service.ApplicationService
import com.pashaoleynik97.droiddeploy.core.storage.ApkStorage
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class ApplicationServiceImpl(
    private val applicationRepository: ApplicationRepository,
    private val apkMetadataExtractor: ApkMetadataExtractor,
    private val apkStorage: ApkStorage
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

    @Transactional
    override fun deleteApplication(id: UUID) {
        logger.debug { "Attempting to delete application by id: $id" }

        // Verify application exists
        val application = applicationRepository.findById(id)
            ?: throw ApplicationNotFoundException(id)

        logger.debug { "Application found: id=${application.id}, name=${application.name}, bundleId=${application.bundleId}" }

        // Get all version codes for this application
        val versionCodes = applicationRepository.findAllVersionCodes(id)
        logger.debug { "Found ${versionCodes.size} version(s) to delete for application $id" }

        // Delete APK files from storage for each version
        for (versionCode in versionCodes) {
            try {
                logger.debug { "Deleting APK file: applicationId=$id, versionCode=$versionCode" }
                apkStorage.deleteApk(id, versionCode.toLong())
                logger.debug { "APK file deleted successfully: versionCode=$versionCode" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to delete APK file for applicationId=$id, versionCode=$versionCode. Continuing with next file." }
                // Continue with next file deletion and database cleanup
            }
        }

        // Delete application (versions will be deleted by DB cascade)
        applicationRepository.deleteById(id)

        logger.info { "Application deleted successfully: id=$id, name=${application.name}, bundleId=${application.bundleId}, deletedVersions=${versionCodes.size}" }
    }

    @Transactional
    override fun uploadNewVersion(applicationId: UUID, apkContent: ByteArray): VersionDto {
        logger.debug { "Attempting to upload new version for application: $applicationId" }

        // 1. Load application by id
        var application = applicationRepository.findById(applicationId)
            ?: throw ApplicationNotFoundException(applicationId)

        logger.debug { "Application found: id=${application.id}, bundleId=${application.bundleId}" }

        // 2. Extract APK metadata
        val metadata = try {
            apkMetadataExtractor.extractMetadata(apkContent)
        } catch (e: Exception) {
            logger.error(e) { "Failed to extract APK metadata for application $applicationId" }
            throw IllegalArgumentException("Failed to parse APK file: ${e.message}", e)
        }

        logger.debug { "APK metadata extracted: versionCode=${metadata.versionCode}, versionName=${metadata.versionName}, certSha256=${metadata.signingCertificateSha256}" }

        // 3. Enforce signing certificate rules
        if (application.signingCertificateSha256 == null) {
            // First upload - set the signing certificate
            logger.info { "Setting signing certificate for application $applicationId: ${metadata.signingCertificateSha256}" }
            application = application.copy(signingCertificateSha256 = metadata.signingCertificateSha256)
            application = applicationRepository.save(application)
        } else if (application.signingCertificateSha256 != metadata.signingCertificateSha256) {
            // Certificate mismatch
            logger.warn { "Signing certificate mismatch for application $applicationId. Expected: ${application.signingCertificateSha256}, Got: ${metadata.signingCertificateSha256}" }
            throw SigningCertificateMismatchException(
                applicationId = applicationId.toString(),
                expectedSha256 = application.signingCertificateSha256!!,
                actualSha256 = metadata.signingCertificateSha256
            )
        } else {
            logger.debug { "Signing certificate matches: ${application.signingCertificateSha256}" }
        }

        // 4. Validate version code
        // Check if version already exists
        if (applicationRepository.versionExists(applicationId, metadata.versionCode)) {
            logger.warn { "Version code ${metadata.versionCode} already exists for application $applicationId" }
            throw ApplicationVersionAlreadyExistsException(
                applicationId = applicationId.toString(),
                versionCode = metadata.versionCode
            )
        }

        // Check that version code is greater than existing max
        val maxVersionCode = applicationRepository.findMaxVersionCode(applicationId)
        if (maxVersionCode != null && metadata.versionCode <= maxVersionCode) {
            logger.warn { "Version code ${metadata.versionCode} is not greater than max version code $maxVersionCode for application $applicationId" }
            throw InvalidVersionCodeException(
                versionCode = metadata.versionCode,
                maxVersionCode = maxVersionCode
            )
        }

        logger.debug { "Version code validation passed. Max existing: $maxVersionCode, new: ${metadata.versionCode}" }

        // 5. Create new ApplicationVersion
        val now = Instant.now()
        val applicationVersion = ApplicationVersion(
            id = UUID.randomUUID(),
            application = application,
            versionCode = metadata.versionCode,
            versionName = metadata.versionName,
            stable = false, // By default, newly uploaded versions are not stable
            createdAt = now
        )

        // 6. Save APK file first (before DB persist)
        var apkSaved = false
        try {
            logger.debug { "Saving APK file to storage: applicationId=$applicationId, versionCode=${metadata.versionCode}" }
            apkStorage.saveApk(applicationId, metadata.versionCode.toLong(), ByteArrayInputStream(apkContent))
            apkSaved = true
            logger.debug { "APK file saved successfully" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to save APK file for application $applicationId, version ${metadata.versionCode}" }
            throw ApkStorageException("Failed to save APK file: ${e.message}", e)
        }

        // 7. Persist ApplicationVersion to database
        val savedVersion = try {
            applicationRepository.saveVersion(applicationVersion)
        } catch (e: Exception) {
            logger.error(e) { "Failed to persist application version to database. Rolling back APK file." }

            // Compensation: attempt to delete the APK file we just saved
            if (apkSaved) {
                try {
                    logger.warn { "Attempting to delete APK file after DB persist failure" }
                    apkStorage.deleteApk(applicationId, metadata.versionCode.toLong())
                    logger.info { "Successfully deleted APK file during compensation" }
                } catch (deleteException: Exception) {
                    logger.error(deleteException) { "Failed to delete APK file during compensation. Manual cleanup may be required." }
                }
            }

            // Re-throw the original exception
            throw e
        }

        logger.info { "Application version uploaded successfully: applicationId=$applicationId, versionCode=${savedVersion.versionCode}, versionName=${savedVersion.versionName}" }

        // 8. Map to DTO and return
        return VersionDto(
            versionCode = savedVersion.versionCode.toLong(),
            versionName = savedVersion.versionName,
            stable = savedVersion.stable
        )
    }

    override fun updateVersionStability(applicationId: UUID, versionCode: Long, stable: Boolean): VersionDto {
        logger.debug { "Attempting to update version stability: applicationId=$applicationId, versionCode=$versionCode, stable=$stable" }

        // 1. Verify application exists
        val application = applicationRepository.findById(applicationId)
            ?: throw ApplicationNotFoundException(applicationId)

        logger.debug { "Application found: id=${application.id}, bundleId=${application.bundleId}" }

        // 2. Find the version
        val version = applicationRepository.findVersion(applicationId, versionCode)
            ?: throw ApplicationVersionNotFoundException(applicationId, versionCode)

        logger.debug { "Version found: versionCode=${version.versionCode}, versionName=${version.versionName}, currentStability=${version.stable}" }

        // 3. Update the version stability
        val updatedVersion = version.copy(stable = stable)

        // 4. Save the updated version
        val savedVersion = applicationRepository.saveVersion(updatedVersion)

        logger.info { "Version stability updated successfully: applicationId=$applicationId, versionCode=$versionCode, stable=$stable" }

        // 5. Map to DTO and return
        return VersionDto(
            versionCode = savedVersion.versionCode.toLong(),
            versionName = savedVersion.versionName,
            stable = savedVersion.stable
        )
    }

    @Transactional
    override fun deleteVersion(applicationId: UUID, versionCode: Long) {
        logger.debug { "Attempting to delete version: applicationId=$applicationId, versionCode=$versionCode" }

        // 1. Verify application exists
        val application = applicationRepository.findById(applicationId)
            ?: throw ApplicationNotFoundException(applicationId)

        logger.debug { "Application found: id=${application.id}, bundleId=${application.bundleId}" }

        // 2. Verify version exists
        val version = applicationRepository.findVersion(applicationId, versionCode)
            ?: throw ApplicationVersionNotFoundException(applicationId, versionCode)

        logger.debug { "Version found: versionCode=${version.versionCode}, versionName=${version.versionName}" }

        // 3. Delete APK file from storage
        try {
            logger.debug { "Deleting APK file from storage: applicationId=$applicationId, versionCode=$versionCode" }
            apkStorage.deleteApk(applicationId, versionCode)
            logger.debug { "APK file deleted successfully" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete APK file for applicationId=$applicationId, versionCode=$versionCode. Continuing with database deletion." }
            // Continue with database deletion even if file deletion fails
        }

        // 4. Delete version from database
        applicationRepository.deleteVersion(applicationId, versionCode)

        logger.info { "Version deleted successfully: applicationId=$applicationId, versionCode=$versionCode" }
    }

    override fun getVersion(applicationId: UUID, versionCode: Long): VersionDto {
        logger.debug { "Attempting to get version: applicationId=$applicationId, versionCode=$versionCode" }

        // 1. Verify application exists
        val application = applicationRepository.findById(applicationId)
            ?: throw ApplicationNotFoundException(applicationId)

        logger.debug { "Application found: id=${application.id}, bundleId=${application.bundleId}" }

        // 2. Find the version
        val version = applicationRepository.findVersion(applicationId, versionCode)
            ?: throw ApplicationVersionNotFoundException(applicationId, versionCode)

        logger.info { "Version found: versionCode=${version.versionCode}, versionName=${version.versionName}, stable=${version.stable}" }

        // 3. Map to DTO and return
        return VersionDto.fromDomain(version)
    }

    override fun listVersions(applicationId: UUID, pageable: Pageable): Page<ApplicationVersion> {
        logger.debug { "Attempting to list versions for application: applicationId=$applicationId, page=${pageable.pageNumber}, size=${pageable.pageSize}" }

        // 1. Verify application exists
        val application = applicationRepository.findById(applicationId)
            ?: throw ApplicationNotFoundException(applicationId)

        logger.debug { "Application found: id=${application.id}, bundleId=${application.bundleId}" }

        // 2. Fetch versions page
        val versionsPage = applicationRepository.findAllVersions(applicationId, pageable)

        logger.info { "Retrieved ${versionsPage.totalElements} versions for application $applicationId, returning page ${versionsPage.number} of ${versionsPage.totalPages}" }

        return versionsPage
    }
}
