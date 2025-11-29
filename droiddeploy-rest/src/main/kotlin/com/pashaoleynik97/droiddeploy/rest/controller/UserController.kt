package com.pashaoleynik97.droiddeploy.rest.controller

import com.pashaoleynik97.droiddeploy.core.config.UserDefaultsProperties
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.core.exception.ForbiddenAccessException
import com.pashaoleynik97.droiddeploy.core.exception.UserNotFoundException
import com.pashaoleynik97.droiddeploy.core.service.UserService
import com.pashaoleynik97.droiddeploy.rest.model.user.CreateUserRequestDto
import com.pashaoleynik97.droiddeploy.rest.model.user.UpdatePasswordRequestDto
import com.pashaoleynik97.droiddeploy.rest.model.user.UserResponseDto
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.PagedResponse
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.RestResponse
import com.pashaoleynik97.droiddeploy.rest.security.JwtAuthentication
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

@RestController
@RequestMapping("/api/v1/user")
class UserController(
    private val userService: UserService,
    private val userDefaultsProperties: UserDefaultsProperties
) {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun listUsers(
        @RequestParam(required = false) role: String?,
        @RequestParam(required = false) isActive: Boolean?,
        @RequestParam(defaultValue = "0") page: Int,
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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createUser(@RequestBody request: CreateUserRequestDto): ResponseEntity<RestResponse<UserResponseDto>> {
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

    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    fun getUserById(
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

    @PutMapping("/{userId}/password")
    @PreAuthorize("hasRole('ADMIN')")
    fun updatePassword(
        @PathVariable userId: UUID,
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
}
