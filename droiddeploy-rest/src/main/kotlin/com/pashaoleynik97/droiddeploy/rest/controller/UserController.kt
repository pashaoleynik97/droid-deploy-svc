package com.pashaoleynik97.droiddeploy.rest.controller

import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.core.service.UserService
import com.pashaoleynik97.droiddeploy.rest.model.user.CreateUserRequestDto
import com.pashaoleynik97.droiddeploy.rest.model.user.UserResponseDto
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.RestResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
}
