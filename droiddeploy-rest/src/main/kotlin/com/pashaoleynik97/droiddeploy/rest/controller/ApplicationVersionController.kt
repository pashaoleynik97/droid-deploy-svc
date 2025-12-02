package com.pashaoleynik97.droiddeploy.rest.controller

import com.pashaoleynik97.droiddeploy.core.dto.application.UpdateVersionStabilityRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.application.VersionDto
import com.pashaoleynik97.droiddeploy.core.service.ApplicationService
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.PagedResponse
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.RestResponse
import mu.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID
import kotlin.math.min

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1/application")
class ApplicationVersionController(
    private val applicationService: ApplicationService
) {

    @GetMapping("/{applicationId}/version")
    @PreAuthorize("hasRole('ADMIN')")
    fun listVersions(
        @PathVariable applicationId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<RestResponse<PagedResponse<VersionDto>>> {
        logger.info { "GET /api/v1/application/{applicationId}/version - List versions request: applicationId=$applicationId, page=$page, size=$size" }

        // Validate page size (max 100)
        val validatedSize = min(size, 100)
        if (size > 100) {
            logger.warn { "Requested page size $size exceeds maximum of 100, using 100" }
        }

        // Create pageable with sort by createdAt descending
        val pageable = PageRequest.of(page, validatedSize, Sort.by(Sort.Direction.DESC, "createdAt"))

        // Fetch versions
        val versionsPage = applicationService.listVersions(applicationId, pageable)
        val pagedResponse = PagedResponse.from(versionsPage, VersionDto::fromDomain)

        logger.info { "Retrieved ${pagedResponse.totalElements} versions for application $applicationId, returning page ${pagedResponse.page} of ${pagedResponse.totalPages}" }

        return ResponseEntity
            .ok(RestResponse.success(pagedResponse, "Versions retrieved successfully"))
    }

    @GetMapping("/{applicationId}/version/{versionCode}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getVersion(
        @PathVariable applicationId: UUID,
        @PathVariable versionCode: Long
    ): ResponseEntity<RestResponse<VersionDto>> {
        logger.info { "GET /api/v1/application/{applicationId}/version/{versionCode} - Get version request: applicationId=$applicationId, versionCode=$versionCode" }

        val versionDto = applicationService.getVersion(applicationId, versionCode)

        logger.info { "Version retrieved successfully: applicationId=$applicationId, versionCode=${versionDto.versionCode}, versionName=${versionDto.versionName}" }

        return ResponseEntity
            .ok(RestResponse.success(versionDto, "Version retrieved successfully"))
    }

    @PostMapping("/{applicationId}/version", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasAnyRole('ADMIN', 'CI')")
    fun uploadVersion(
        @PathVariable applicationId: UUID,
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<RestResponse<VersionDto>> {
        logger.info { "POST /api/v1/application/{applicationId}/version - Upload version request: applicationId=$applicationId, fileName=${file.originalFilename}, size=${file.size}" }

        // Validate file
        if (file.isEmpty) {
            logger.warn { "Failed to upload version: file is empty" }
            throw IllegalArgumentException("APK file is required")
        }

        // Upload new version
        val versionDto = applicationService.uploadNewVersion(applicationId, file.bytes)

        logger.info { "Version uploaded successfully: applicationId=$applicationId, versionCode=${versionDto.versionCode}, versionName=${versionDto.versionName}" }

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(RestResponse.success(versionDto, "Version uploaded successfully"))
    }

    @PutMapping("/{applicationId}/version/{versionCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CI')")
    fun updateVersionStability(
        @PathVariable applicationId: UUID,
        @PathVariable versionCode: Long,
        @RequestBody request: UpdateVersionStabilityRequestDto
    ): ResponseEntity<RestResponse<VersionDto>> {
        logger.info { "PUT /api/v1/application/{applicationId}/version/{versionCode} - Update version stability request: applicationId=$applicationId, versionCode=$versionCode, stable=${request.stable}" }

        val versionDto = applicationService.updateVersionStability(applicationId, versionCode, request.stable)

        logger.info { "Version stability updated successfully: applicationId=$applicationId, versionCode=$versionCode, stable=${versionDto.stable}" }

        return ResponseEntity
            .ok(RestResponse.success(versionDto, "Version stability updated successfully"))
    }

    @DeleteMapping("/{applicationId}/version/{versionCode}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteVersion(
        @PathVariable applicationId: UUID,
        @PathVariable versionCode: Long
    ): ResponseEntity<RestResponse<Nothing>> {
        logger.info { "DELETE /api/v1/application/{applicationId}/version/{versionCode} - Delete version request: applicationId=$applicationId, versionCode=$versionCode" }

        applicationService.deleteVersion(applicationId, versionCode)

        logger.info { "Version deleted successfully: applicationId=$applicationId, versionCode=$versionCode" }

        return ResponseEntity
            .ok(RestResponse.success(null, "Deleted"))
    }
}
