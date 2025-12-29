package com.pashaoleynik97.droiddeploy.rest.controller

import com.pashaoleynik97.droiddeploy.core.dto.application.UpdateVersionStabilityRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.application.VersionDto
import com.pashaoleynik97.droiddeploy.core.service.ApplicationService
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.PagedResponse
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.RestResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID
import kotlin.math.min

private val logger = KotlinLogging.logger {}

@Tag(
    name = "Application Versions",
    description = "APK version management endpoints. Versions represent specific releases of an application. " +
            "Each version contains an APK file and metadata (version code, version name, stability flag). " +
            "Endpoints have varying security requirements: ADMIN for management, ADMIN/CONSUMER for downloads, ADMIN/CI for uploads."
)
@RestController
@RequestMapping("/api/v1/application")
class ApplicationVersionController(
    private val applicationService: ApplicationService
) {

    @Operation(
        summary = "List application versions",
        description = "Retrieve a paginated list of all versions for a specific application. " +
                "Results are sorted by creation date in descending order (newest first). " +
                "Page size is capped at 100 items. Requires ADMIN role."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Versions retrieved successfully",
                
            ),
            ApiResponse(
                responseCode = "404",
                description = "Application not found",
                
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - ADMIN role required",
                
            )
        ]
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{applicationId}/version")
    @PreAuthorize("hasRole('ADMIN')")
    fun listVersions(
        @Parameter(description = "Application UUID", required = true)
        @PathVariable applicationId: UUID,
        @Parameter(description = "Page number (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Number of items per page (maximum 100)", example = "20")
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

    @Operation(
        summary = "Get latest stable version",
        description = "Retrieve the latest stable version of an application. " +
                "Returns the most recent version marked as stable. " +
                "Used by consumer applications to check for updates. " +
                "Requires ADMIN or CONSUMER role (can be accessed with API key)."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Latest stable version retrieved successfully",
                
            ),
            ApiResponse(
                responseCode = "404",
                description = "Application not found or no stable versions available",
                
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - ADMIN or CONSUMER role required",
                
            )
        ]
    )
    @SecurityRequirement(name = "bearerAuth")
    @SecurityRequirement(name = "apiKeyAuth")
    @GetMapping("/{applicationId}/version/latest")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONSUMER')")
    fun getLatestVersion(
        @Parameter(description = "Application UUID", required = true)
        @PathVariable applicationId: UUID
    ): ResponseEntity<RestResponse<VersionDto>> {
        logger.info { "GET /api/v1/application/{applicationId}/version/latest - Get latest version request: applicationId=$applicationId" }

        val versionDto = applicationService.getLatestVersion(applicationId)

        logger.info { "Latest version retrieved successfully: applicationId=$applicationId, versionCode=${versionDto.versionCode}, versionName=${versionDto.versionName}" }

        return ResponseEntity
            .ok(RestResponse.success(versionDto, "Latest version retrieved successfully"))
    }

    @Operation(
        summary = "Get specific version by version code",
        description = "Retrieve detailed information about a specific version by its version code. " +
                "Version code is a numeric identifier from the APK manifest. Requires ADMIN role."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Version retrieved successfully",
                
            ),
            ApiResponse(
                responseCode = "404",
                description = "Application or version not found",
                
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - ADMIN role required",
                
            )
        ]
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{applicationId}/version/{versionCode}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getVersion(
        @Parameter(description = "Application UUID", required = true)
        @PathVariable applicationId: UUID,
        @Parameter(description = "Version code (numeric identifier from APK)", required = true, example = "123")
        @PathVariable versionCode: Long
    ): ResponseEntity<RestResponse<VersionDto>> {
        logger.info { "GET /api/v1/application/{applicationId}/version/{versionCode} - Get version request: applicationId=$applicationId, versionCode=$versionCode" }

        val versionDto = applicationService.getVersion(applicationId, versionCode)

        logger.info { "Version retrieved successfully: applicationId=$applicationId, versionCode=${versionDto.versionCode}, versionName=${versionDto.versionName}" }

        return ResponseEntity
            .ok(RestResponse.success(versionDto, "Version retrieved successfully"))
    }

    @Operation(
        summary = "Download APK file",
        description = "Download the APK binary file for a specific version. " +
                "Returns the APK file as a binary download with appropriate content-type and Content-Disposition headers. " +
                "Used by consumer applications to download app updates. " +
                "Requires ADMIN or CONSUMER role (can be accessed with API key)."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "APK file downloaded successfully",
                content = [Content(
                    mediaType = "application/vnd.android.package-archive",
                    schema = Schema(type = "string", format = "binary")
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Application or version not found",
                
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - ADMIN or CONSUMER role required",
                
            )
        ]
    )
    @SecurityRequirement(name = "bearerAuth")
    @SecurityRequirement(name = "apiKeyAuth")
    @GetMapping("/{applicationId}/version/{versionCode}/apk")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONSUMER')")
    fun downloadApk(
        @Parameter(description = "Application UUID", required = true)
        @PathVariable applicationId: UUID,
        @Parameter(description = "Version code (numeric identifier from APK)", required = true, example = "123")
        @PathVariable versionCode: Long
    ): ResponseEntity<InputStreamResource> {
        logger.info { "GET /api/v1/application/{applicationId}/version/{versionCode}/apk - Download APK request: applicationId=$applicationId, versionCode=$versionCode" }

        val apkStream = applicationService.getApkStream(applicationId, versionCode)
        val resource = InputStreamResource(apkStream.inputStream)

        logger.info { "APK download initiated: applicationId=$applicationId, versionCode=$versionCode, fileName=${apkStream.fileName}" }

        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${apkStream.fileName}\"")
            .body(resource)
    }

    @Operation(
        summary = "Upload new APK version",
        description = "Upload and register a new APK version for an application. " +
                "The APK file is parsed to extract version code, version name, and other metadata. " +
                "The file is stored and a new version record is created. " +
                "Newly uploaded versions are marked as unstable by default. Use the update stability endpoint to mark as stable. " +
                "Requires ADMIN or CI role (can be accessed with API key for CI/CD pipelines)."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Version uploaded successfully",
                
            ),
            ApiResponse(
                responseCode = "404",
                description = "Application not found",
                
            ),
            ApiResponse(
                responseCode = "409",
                description = "Conflict - Version with this version code already exists",
                
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - ADMIN or CI role required",
                
            )
        ]
    )
    @SecurityRequirement(name = "bearerAuth")
    @SecurityRequirement(name = "apiKeyAuth")
    @PostMapping("/{applicationId}/version", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasAnyRole('ADMIN', 'CI')")
    fun uploadVersion(
        @Parameter(description = "Application UUID", required = true)
        @PathVariable applicationId: UUID,
        @Parameter(
            description = "APK file to upload",
            required = true,
            content = [Content(mediaType = "multipart/form-data")]
        )
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

    @Operation(
        summary = "Update version stability flag",
        description = "Mark a version as stable or unstable. " +
                "Stable versions are returned by the 'latest version' endpoint and are recommended for end users. " +
                "Unstable versions are for testing and won't be returned as the latest version. " +
                "Requires ADMIN or CI role (can be accessed with API key for CI/CD pipelines)."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Version stability updated successfully",
                
            ),
            ApiResponse(
                responseCode = "404",
                description = "Application or version not found",
                
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - ADMIN or CI role required",
                
            )
        ]
    )
    @SecurityRequirement(name = "bearerAuth")
    @SecurityRequirement(name = "apiKeyAuth")
    @PutMapping("/{applicationId}/version/{versionCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CI')")
    fun updateVersionStability(
        @Parameter(description = "Application UUID", required = true)
        @PathVariable applicationId: UUID,
        @Parameter(description = "Version code (numeric identifier from APK)", required = true, example = "123")
        @PathVariable versionCode: Long,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Stability flag update (true for stable, false for unstable)",
            required = true
        )
        @RequestBody request: UpdateVersionStabilityRequestDto
    ): ResponseEntity<RestResponse<VersionDto>> {
        logger.info { "PUT /api/v1/application/{applicationId}/version/{versionCode} - Update version stability request: applicationId=$applicationId, versionCode=$versionCode, stable=${request.stable}" }

        val versionDto = applicationService.updateVersionStability(applicationId, versionCode, request.stable)

        logger.info { "Version stability updated successfully: applicationId=$applicationId, versionCode=$versionCode, stable=${versionDto.stable}" }

        return ResponseEntity
            .ok(RestResponse.success(versionDto, "Version stability updated successfully"))
    }

    @Operation(
        summary = "Delete version",
        description = "Permanently delete a version and its associated APK file from storage. " +
                "This operation cannot be undone. The version record and APK file will be deleted. " +
                "Requires ADMIN role."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Version deleted successfully",
                
            ),
            ApiResponse(
                responseCode = "404",
                description = "Application or version not found",
                
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - ADMIN role required",
                
            )
        ]
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{applicationId}/version/{versionCode}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteVersion(
        @Parameter(description = "Application UUID", required = true)
        @PathVariable applicationId: UUID,
        @Parameter(description = "Version code (numeric identifier from APK)", required = true, example = "123")
        @PathVariable versionCode: Long
    ): ResponseEntity<RestResponse<Nothing>> {
        logger.info { "DELETE /api/v1/application/{applicationId}/version/{versionCode} - Delete version request: applicationId=$applicationId, versionCode=$versionCode" }

        applicationService.deleteVersion(applicationId, versionCode)

        logger.info { "Version deleted successfully: applicationId=$applicationId, versionCode=$versionCode" }

        return ResponseEntity
            .ok(RestResponse.success(null, "Deleted"))
    }
}
