package com.pashaoleynik97.droiddeploy.rest.controller

import com.pashaoleynik97.droiddeploy.core.dto.application.UpdateVersionStabilityRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.application.VersionDto
import com.pashaoleynik97.droiddeploy.core.service.ApplicationService
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.RestResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1/application")
class ApplicationVersionController(
    private val applicationService: ApplicationService
) {

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
}
