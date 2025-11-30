package com.pashaoleynik97.droiddeploy.rest.controller

import com.pashaoleynik97.droiddeploy.core.dto.auth.LoginRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.auth.RefreshTokenRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.auth.TokenPairDto
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.RestResponse
import com.pashaoleynik97.droiddeploy.core.service.AuthService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequestDto): ResponseEntity<RestResponse<TokenPairDto>> {
        logger.info { "POST /api/v1/auth/login - Login request received for user: ${request.login}" }

        val tokenPair = authService.login(request)

        logger.info { "Login successful for user: ${request.login}" }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(tokenPair, "Login successful"))
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshTokenRequestDto): ResponseEntity<RestResponse<TokenPairDto>> {
        logger.info { "POST /api/v1/auth/refresh - Refresh token request received" }

        val tokenPair = authService.refreshToken(request)

        logger.info { "Refresh token successful" }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(tokenPair, "Token refreshed successfully"))
    }
}
