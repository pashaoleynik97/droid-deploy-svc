package com.pashaoleynik97.droiddeploy.rest.controller

import com.pashaoleynik97.droiddeploy.core.dto.application.ApplicationResponseDto
import com.pashaoleynik97.droiddeploy.core.dto.application.CreateApplicationRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.application.UpdateApplicationRequestDto
import com.pashaoleynik97.droiddeploy.core.service.ApplicationService
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.PagedResponse
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.RestResponse
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

@RestController
@RequestMapping("/api/v1/application")
class ApplicationController(
    private val applicationService: ApplicationService
) {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun listApplications(
        @RequestParam(defaultValue = "0") page: Int,
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

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getApplicationById(
        @PathVariable id: UUID
    ): ResponseEntity<RestResponse<ApplicationResponseDto>> {
        logger.info { "GET /api/v1/application/{id} - Get application by ID: id=$id" }

        val application = applicationService.getApplicationById(id)

        logger.info { "Application retrieved successfully: id=${application.id}, bundleId=${application.bundleId}" }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(application, "Application retrieved successfully"))
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createApplication(
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

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateApplication(
        @PathVariable id: UUID,
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
}
