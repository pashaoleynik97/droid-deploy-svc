package com.pashaoleynik97.droiddeploy.rest.controller

import com.pashaoleynik97.droiddeploy.core.domain.ApiKeyRole
import com.pashaoleynik97.droiddeploy.core.dto.apikey.ApiKeyDto
import com.pashaoleynik97.droiddeploy.core.dto.apikey.CreateApiKeyRequestDto
import com.pashaoleynik97.droiddeploy.core.service.ApiKeyService
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.PagedResponse
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.RestResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
import java.util.*
import kotlin.math.min

private val logger = KotlinLogging.logger {}

@Tag(
    name = "API Keys",
    description = "Application API key management endpoints. All endpoints require ADMIN role. " +
            "API keys allow CI/CD pipelines and consumer applications to authenticate without user credentials. " +
            "Each API key belongs to an application and has a role (CI or CONSUMER) that determines its permissions."
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/application/{applicationId}/security/apikey")
class ApplicationApiKeyController(
    private val apiKeyService: ApiKeyService
) {

    @Operation(
        summary = "Create new API key",
        description = "Generate a new API key for an application with specified role (CI or CONSUMER). " +
                "The generated key is returned only once during creation and cannot be retrieved later. " +
                "Store it securely. API keys can be used via the /auth/apikey endpoint to obtain access tokens. " +
                "Requires ADMIN role."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "API key created successfully. The key value is returned only once.",
                
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
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createApiKey(
        @Parameter(description = "Application UUID", required = true)
        @PathVariable applicationId: UUID,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "API key details (name and role - CI or CONSUMER)",
            required = true
        )
        @RequestBody request: CreateApiKeyRequestDto
    ): ResponseEntity<RestResponse<ApiKeyDto>> {
        logger.info { "POST /api/v1/application/$applicationId/security/apikey - Create API key request: name=${request.name}, role=${request.role}" }

        val apiKey = apiKeyService.createApiKey(applicationId, request)

        logger.info { "API key created successfully: id=${apiKey.id}, applicationId=$applicationId" }

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(RestResponse.success(apiKey, "API key created successfully"))
    }

    @Operation(
        summary = "List API keys for application",
        description = "Retrieve a paginated list of API keys for a specific application with optional filtering and sorting. " +
                "Supports filtering by: role (CI, CONSUMER), isActive (true/false). " +
                "Supports custom sorting (format: field,direction). " +
                "Page size is capped at 100 items. Requires ADMIN role."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "API keys retrieved successfully",
                
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
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun listApiKeys(
        @Parameter(description = "Application UUID", required = true)
        @PathVariable applicationId: UUID,
        @Parameter(description = "Filter by API key role", schema = Schema(allowableValues = ["CI", "CONSUMER"]))
        @RequestParam(required = false) role: String?,
        @Parameter(description = "Filter by active status")
        @RequestParam(required = false, defaultValue = "false") isActive: Boolean?,
        @Parameter(description = "Page number (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Number of items per page (maximum 100)", example = "20")
        @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Sort order (format: field,direction)", example = "createdAt,desc")
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

    @Operation(
        summary = "Revoke API key",
        description = "Revoke (deactivate) an API key, preventing it from being used for authentication. " +
                "Revoked keys cannot be reactivated - you must create a new key if needed. " +
                "This operation is permanent. Requires ADMIN role."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "API key revoked successfully",
                
            ),
            ApiResponse(
                responseCode = "404",
                description = "Application or API key not found",
                
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - ADMIN role required",
                
            )
        ]
    )
    @PostMapping("/{apiKeyId}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    fun revokeApiKey(
        @Parameter(description = "Application UUID", required = true)
        @PathVariable applicationId: UUID,
        @Parameter(description = "API Key UUID", required = true)
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
