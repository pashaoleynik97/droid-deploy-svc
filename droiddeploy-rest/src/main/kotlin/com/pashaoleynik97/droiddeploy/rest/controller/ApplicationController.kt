package com.pashaoleynik97.droiddeploy.rest.controller

import com.pashaoleynik97.droiddeploy.core.dto.application.ApplicationResponseDto
import com.pashaoleynik97.droiddeploy.core.dto.application.CreateApplicationRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.application.UpdateApplicationRequestDto
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
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID
import kotlin.math.min

private val logger = KotlinLogging.logger {}

@Tag(
    name = "Applications",
    description = "Application management endpoints. All endpoints require ADMIN role. " +
            "Applications represent Android apps registered in the system. Each application can have multiple versions (APKs)."
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/application")
class ApplicationController(
    private val applicationService: ApplicationService
) {

    @Operation(
        summary = "List all applications",
        description = "Retrieve a paginated list of all applications registered in the system. " +
                "Results are sorted by creation date in descending order (newest first). " +
                "Page size is capped at 100 items."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Applications retrieved successfully"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - ADMIN role required"
            )
        ]
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun listApplications(
        @Parameter(description = "Page number (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Number of items per page (maximum 100)", example = "20")
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<RestResponse<PagedResponse<ApplicationResponseDto>>> {
        logger.info { "GET /api/v1/application - List applications request: page=$page, size=$size" }

        // Validate page size (max 100)
        val validatedSize = min(size, 100)
        if (size > 100) {
            logger.warn { "Requested page size $size exceeds maximum of 100, using 100" }
        }

        // Create pageable with sort by createdAt descending
        val pageable = PageRequest.of(page, validatedSize, Sort.by(Sort.Direction.DESC, "createdAt"))

        // Fetch applications
        val applicationsPage = applicationService.listApplications(pageable)
        val pagedResponse = PagedResponse.from(applicationsPage, ApplicationResponseDto::fromDomain)

        logger.info { "Retrieved ${pagedResponse.totalElements} applications, returning page ${pagedResponse.page} of ${pagedResponse.totalPages}" }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(pagedResponse, "Applications retrieved successfully"))
    }

    @Operation(
        summary = "Get application by ID",
        description = "Retrieve detailed information about a specific application by its unique identifier (UUID)."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Application retrieved successfully"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Application not found"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - ADMIN role required"
            )
        ]
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getApplicationById(
        @Parameter(description = "Application UUID", required = true)
        @PathVariable id: UUID
    ): ResponseEntity<RestResponse<ApplicationResponseDto>> {
        logger.info { "GET /api/v1/application/{id} - Get application by ID: id=$id" }

        val application = applicationService.getApplicationById(id)

        logger.info { "Application retrieved successfully: id=${application.id}, bundleId=${application.bundleId}" }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(application, "Application retrieved successfully"))
    }

    @Operation(
        summary = "Create new application",
        description = "Register a new Android application in the system. " +
                "The bundle ID (e.g., com.example.app) must be unique. " +
                "After creation, you can upload APK versions for this application."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Application created successfully"
            ),
            ApiResponse(
                responseCode = "409",
                description = "Conflict - Application with this bundle ID already exists"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - ADMIN role required"
            )
        ]
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createApplication(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Application details (name and unique bundle ID)",
            required = true
        )
        @RequestBody request: CreateApplicationRequestDto
    ): ResponseEntity<RestResponse<ApplicationResponseDto>> {
        logger.info { "POST /api/v1/application - Create application request: name=${request.name}, bundleId=${request.bundleId}" }

        val application = applicationService.createApplication(request)
        val responseDto = ApplicationResponseDto.fromDomain(application)

        logger.info { "Application created successfully: id=${application.id}, bundleId=${application.bundleId}" }

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(RestResponse.success(responseDto, "Application created successfully"))
    }

    @Operation(
        summary = "Update application",
        description = "Update application name and/or bundle ID. " +
                "If changing the bundle ID, the new value must be unique across all applications."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Application updated successfully"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Application not found"
            ),
            ApiResponse(
                responseCode = "409",
                description = "Conflict - New bundle ID already exists for another application"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - ADMIN role required"
            )
        ]
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateApplication(
        @Parameter(description = "Application UUID", required = true)
        @PathVariable id: UUID,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Updated application details",
            required = true
        )
        @RequestBody request: UpdateApplicationRequestDto
    ): ResponseEntity<RestResponse<ApplicationResponseDto>> {
        logger.info { "PUT /api/v1/application/{id} - Update application request: id=$id, name=${request.name}, bundleId=${request.bundleId}" }

        val application = applicationService.updateApplication(id, request)
        val responseDto = ApplicationResponseDto.fromDomain(application)

        logger.info { "Application updated successfully: id=${application.id}, bundleId=${application.bundleId}" }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(responseDto, "Application updated successfully"))
    }

    @Operation(
        summary = "Delete application",
        description = "Permanently delete an application and all its associated versions and APK files. " +
                "This operation cannot be undone. All version data and APK files will be deleted from storage."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Application deleted successfully"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Application not found"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - ADMIN role required"
            )
        ]
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteApplication(
        @Parameter(description = "Application UUID", required = true)
        @PathVariable id: UUID
    ): ResponseEntity<RestResponse<Unit>> {
        logger.info { "DELETE /api/v1/application/{id} - Delete application request: id=$id" }

        applicationService.deleteApplication(id)

        logger.info { "Application deleted successfully: id=$id" }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(Unit, "Application deleted successfully"))
    }
}
