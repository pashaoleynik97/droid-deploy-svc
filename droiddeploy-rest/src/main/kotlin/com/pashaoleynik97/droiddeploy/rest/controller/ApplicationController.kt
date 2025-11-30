package com.pashaoleynik97.droiddeploy.rest.controller

import com.pashaoleynik97.droiddeploy.core.dto.application.ApplicationResponseDto
import com.pashaoleynik97.droiddeploy.core.dto.application.CreateApplicationRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.application.UpdateApplicationRequestDto
import com.pashaoleynik97.droiddeploy.core.service.ApplicationService
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.RestResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1/application")
class ApplicationController(
    private val applicationService: ApplicationService
) {

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
