package com.pashaoleynik97.droiddeploy.rest.controller

import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.core.exception.ForbiddenAccessException
import com.pashaoleynik97.droiddeploy.core.exception.UserNotFoundException
import com.pashaoleynik97.droiddeploy.core.service.UserService
import com.pashaoleynik97.droiddeploy.rest.model.user.CreateUserRequestDto
import com.pashaoleynik97.droiddeploy.rest.model.user.UserResponseDto
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.RestResponse
import com.pashaoleynik97.droiddeploy.rest.security.JwtAuthentication
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1/user")
class UserController(
    private val userService: UserService
) {

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
}
