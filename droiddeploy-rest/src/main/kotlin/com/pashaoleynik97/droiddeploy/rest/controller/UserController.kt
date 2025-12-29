package com.pashaoleynik97.droiddeploy.rest.controller

import com.pashaoleynik97.droiddeploy.core.config.UserDefaultsProperties
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.core.exception.ForbiddenAccessException
import com.pashaoleynik97.droiddeploy.core.exception.UserNotFoundException
import com.pashaoleynik97.droiddeploy.core.service.UserService
import com.pashaoleynik97.droiddeploy.core.dto.user.CreateUserRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.user.UpdatePasswordRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.user.UpdateUserActiveStatusRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.user.UserResponseDto
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.PagedResponse
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.RestResponse
import com.pashaoleynik97.droiddeploy.rest.security.JwtAuthentication
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
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID
import kotlin.math.min

private val logger = KotlinLogging.logger {}

@Tag(
    name = "Users",
    description = "User management endpoints. Users have roles (ADMIN, CI, CONSUMER) that determine their permissions. " +
            "Most endpoints require ADMIN role. Some endpoints have additional authorization logic (e.g., users can only access their own data, super admin restrictions)."
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/user")
class UserController(
    private val userService: UserService,
    private val userDefaultsProperties: UserDefaultsProperties
) {

    @Operation(
        summary = "List users with filters",
        description = "Retrieve a paginated list of users with optional filtering by role and active status. " +
                "Supports filtering by: role (ADMIN, CI, CONSUMER), isActive (true/false). " +
                "Page size is capped at 100 items. Requires ADMIN role."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Users retrieved successfully",
                
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - ADMIN role required",
                
            )
        ]
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun listUsers(
        @Parameter(description = "Filter by user role", schema = Schema(allowableValues = ["ADMIN", "CI", "CONSUMER"]))
        @RequestParam(required = false) role: String?,
        @Parameter(description = "Filter by active status")
        @RequestParam(required = false) isActive: Boolean?,
        @Parameter(description = "Page number (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Number of items per page (maximum 100)", example = "20")
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<RestResponse<PagedResponse<UserResponseDto>>> {
        logger.info { "GET /api/v1/user - List users request with filters: role=$role, isActive=$isActive, page=$page, size=$size" }

        // Validate and convert role parameter
        val roleFilter = role?.let {
            try {
                UserRole.valueOf(it.uppercase())
            } catch (_: IllegalArgumentException) {
                logger.warn { "Invalid role filter provided: $role" }
                throw IllegalArgumentException("Invalid role: $role. Allowed values: ADMIN, CI, CONSUMER")
            }
        }

        // Validate page size (max 100)
        val validatedSize = min(size, 100)
        if (size > 100) {
            logger.warn { "Requested page size $size exceeds maximum of 100, using 100" }
        }

        // Create pageable
        val pageable = PageRequest.of(page, validatedSize)

        // Fetch users
        val usersPage = userService.findAll(roleFilter, isActive, pageable)
        val pagedResponse = PagedResponse.from(usersPage, UserResponseDto::fromDomain)

        logger.info { "Retrieved ${pagedResponse.totalElements} users, returning page ${pagedResponse.page} of ${pagedResponse.totalPages}" }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(pagedResponse, "Users retrieved successfully"))
    }

    @Operation(
        summary = "Create new user",
        description = "Register a new user with ADMIN or CI role. " +
                "CONSUMER users cannot be created via this API (they are created through API key authentication). " +
                "Password must meet strength requirements (minimum 12 characters). " +
                "Requires ADMIN role."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "User created successfully",
                
            ),
            ApiResponse(
                responseCode = "409",
                description = "Conflict - User with this login already exists",
                
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - ADMIN role required",
                
            )
        ]
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createUser(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User details (login, password, role - ADMIN or CI only)",
            required = true
        )
        @RequestBody request: CreateUserRequestDto
    ): ResponseEntity<RestResponse<UserResponseDto>> {
        logger.info { "POST /api/v1/user - Create user request received for login: ${request.login}, role: ${request.role}" }

        val role = try {
            UserRole.valueOf(request.role.uppercase())
        } catch (_: IllegalArgumentException) {
            logger.warn { "Invalid role provided: ${request.role}" }
            throw IllegalArgumentException("Invalid role: ${request.role}. Allowed values: ADMIN, CI")
        }

        val user = userService.createUser(request.login, request.password, role)
        val responseDto = UserResponseDto.fromDomain(user)

        logger.info { "User created successfully: login=${user.login}, id=${user.id}, role=${user.role}" }

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(RestResponse.success(responseDto, "User created successfully"))
    }

    @Operation(
        summary = "Get user by ID",
        description = "Retrieve user details by UUID. " +
                "Authorization: ADMIN can access any user, non-ADMIN users can only access their own data. " +
                "Requires authentication (any role)."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "User retrieved successfully",
                
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found",
                
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - You can only access your own user data (unless you are ADMIN)",
                
            )
        ]
    )
    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    fun getUserById(
        @Parameter(description = "User UUID", required = true)
        @PathVariable userId: UUID,
        @AuthenticationPrincipal authentication: JwtAuthentication
    ): ResponseEntity<RestResponse<UserResponseDto>> {
        logger.info { "GET /api/v1/user/$userId - Get user request from user: ${authentication.userId}, role: ${authentication.userRole}" }

        // Authorization check: ADMIN can access any user, others can only access themselves
        if (authentication.userRole != UserRole.ADMIN && authentication.userId != userId) {
            logger.warn { "User ${authentication.userId} attempted to access user $userId without permission" }
            throw ForbiddenAccessException("You can only access your own user data")
        }

        // Fetch user from service
        val user = userService.findById(userId)
            ?: throw UserNotFoundException(userId)

        val responseDto = UserResponseDto.fromDomain(user)

        logger.info { "User retrieved successfully: id=${user.id}, login=${user.login}" }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(responseDto, "User retrieved successfully"))
    }

    @Operation(
        summary = "Update user password",
        description = "Update password for a user. " +
                "Authorization: Super admin can update any ADMIN user's password, regular admin can only update their own password. " +
                "Password must meet strength requirements (minimum 12 characters). " +
                "Requires ADMIN role."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Password updated successfully",
                
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found",
                
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - You can only update your own password (unless you are super admin)",
                
            )
        ]
    )
    @PutMapping("/{userId}/password")
    @PreAuthorize("hasRole('ADMIN')")
    fun updatePassword(
        @Parameter(description = "User UUID", required = true)
        @PathVariable userId: UUID,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "New password (minimum 12 characters)",
            required = true
        )
        @RequestBody request: UpdatePasswordRequestDto,
        @AuthenticationPrincipal authentication: JwtAuthentication
    ): ResponseEntity<RestResponse<Unit>> {
        logger.info { "PUT /api/v1/user/$userId/password - Update password request from user: ${authentication.userId}, role: ${authentication.userRole}" }

        // Check if current user is super admin
        val superAdmin = userService.findByLogin(userDefaultsProperties.superAdminLogin)
        val isSuperAdmin = superAdmin?.id == authentication.userId

        // Authorization check: Super admin can update any ADMIN user's password, regular admin can only update their own
        if (!isSuperAdmin && authentication.userId != userId) {
            logger.warn { "User ${authentication.userId} attempted to update password for user $userId without permission" }
            throw ForbiddenAccessException("You can only update your own password")
        }

        // Update password (this will validate user exists, is ADMIN, and password strength)
        userService.updatePassword(userId, request.newPassword)

        logger.info { "Password updated successfully for user: $userId by user: ${authentication.userId}" }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(Unit, "Password updated successfully"))
    }

    @Operation(
        summary = "Activate or deactivate user",
        description = "Change user active status. Inactive users cannot authenticate. " +
                "Authorization: Cannot deactivate yourself. Cannot modify super admin. " +
                "Requires ADMIN role."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "User active status updated successfully",
                
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found",
                
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - Cannot deactivate yourself or modify super admin",
                
            )
        ]
    )
    @PutMapping("/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateActiveStatus(
        @Parameter(description = "User UUID", required = true)
        @PathVariable userId: UUID,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Active status update (true to activate, false to deactivate)",
            required = true
        )
        @RequestBody request: UpdateUserActiveStatusRequestDto,
        @AuthenticationPrincipal authentication: JwtAuthentication
    ): ResponseEntity<RestResponse<Unit>> {
        logger.info { "PUT /api/v1/user/$userId/activate - Update active status to ${request.setActive} request from user: ${authentication.userId}" }

        // Update active status (this will validate user exists, not self-modifying, not super admin)
        userService.updateActiveStatus(userId, request.setActive, authentication.userId, userDefaultsProperties.superAdminLogin)

        val statusText = if (request.setActive) "activated" else "deactivated"
        logger.info { "User $userId $statusText successfully by user: ${authentication.userId}" }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(Unit, "User $statusText successfully"))
    }
}
