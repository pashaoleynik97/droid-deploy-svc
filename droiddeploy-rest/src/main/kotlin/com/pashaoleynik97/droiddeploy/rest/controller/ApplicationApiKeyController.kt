package com.pashaoleynik97.droiddeploy.rest.controller

import com.pashaoleynik97.droiddeploy.core.domain.ApiKeyRole
import com.pashaoleynik97.droiddeploy.core.dto.apikey.ApiKeyDto
import com.pashaoleynik97.droiddeploy.core.dto.apikey.CreateApiKeyRequestDto
import com.pashaoleynik97.droiddeploy.core.service.ApiKeyService
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
@RequestMapping("/api/v1/application/{applicationId}/security/apikey")
class ApplicationApiKeyController(
    private val apiKeyService: ApiKeyService
) {

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createApiKey(
        @PathVariable applicationId: UUID,
        @RequestBody request: CreateApiKeyRequestDto
    ): ResponseEntity<RestResponse<ApiKeyDto>> {
        logger.info { "POST /api/v1/application/$applicationId/security/apikey - Create API key request: name=${request.name}, role=${request.role}" }

        val apiKey = apiKeyService.createApiKey(applicationId, request)

        logger.info { "API key created successfully: id=${apiKey.id}, applicationId=$applicationId" }

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(RestResponse.success(apiKey, "API key created successfully"))
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun listApiKeys(
        @PathVariable applicationId: UUID,
        @RequestParam(required = false) role: String?,
        @RequestParam(required = false, defaultValue = "false") isActive: Boolean?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt,desc") sort: String
    ): ResponseEntity<RestResponse<PagedResponse<ApiKeyDto>>> {
        logger.info { "GET /api/v1/application/$applicationId/security/apikey - List API keys request: role=$role, isActive=$isActive, page=$page, size=$size" }

        // Validate page size (max 100)
        val validatedSize = min(size, 100)
        if (size > 100) {
            logger.warn { "Requested page size $size exceeds maximum of 100, using 100" }
        }

        // Parse role if provided
        val apiKeyRole = role?.let {
            try {
                ApiKeyRole.valueOf(it)
            } catch (e: IllegalArgumentException) {
                logger.warn { "Invalid role provided: $role" }
                null
            }
        }

        // Parse sort parameter
        val sortParts = sort.split(",")
        val sortField = sortParts.getOrNull(0) ?: "createdAt"
        val sortDirection = sortParts.getOrNull(1)?.let {
            if (it.equals("asc", ignoreCase = true)) Sort.Direction.ASC else Sort.Direction.DESC
        } ?: Sort.Direction.DESC

        // Create pageable with sort
        val pageable = PageRequest.of(page, validatedSize, Sort.by(sortDirection, sortField))

        // Default behavior: if isActive parameter is not explicitly provided or is false, return everything
        val isActiveFilter = if (isActive == true) true else null

        // Fetch API keys
        val apiKeysPage = apiKeyService.listApiKeys(applicationId, apiKeyRole, isActiveFilter, pageable)
        val pagedResponse = PagedResponse.from(apiKeysPage) { it }

        logger.info { "Retrieved ${pagedResponse.totalElements} API keys for application $applicationId, returning page ${pagedResponse.page} of ${pagedResponse.totalPages}" }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(pagedResponse, "API keys retrieved successfully"))
    }

    @PostMapping("/{apiKeyId}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    fun revokeApiKey(
        @PathVariable applicationId: UUID,
        @PathVariable apiKeyId: UUID
    ): ResponseEntity<RestResponse<Map<String, String>>> {
        logger.info { "POST /api/v1/application/$applicationId/security/apikey/$apiKeyId/revoke - Revoke API key request" }

        apiKeyService.revokeApiKey(applicationId, apiKeyId)

        logger.info { "API key revoked successfully: id=$apiKeyId" }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(mapOf("message" to "OK"), "API key revoked successfully"))
    }
}
