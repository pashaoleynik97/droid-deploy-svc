package com.pashaoleynik97.droiddeploy.rest.controller

import com.pashaoleynik97.droiddeploy.core.dto.application.ApplicationResponseDto
import com.pashaoleynik97.droiddeploy.core.dto.application.CreateApplicationRequestDto
import com.pashaoleynik97.droiddeploy.core.service.ApplicationService
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.RestResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

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
}
