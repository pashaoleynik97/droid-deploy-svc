package com.pashaoleynik97.droiddeploy.rest.controller

import com.pashaoleynik97.droiddeploy.core.dto.auth.ApiKeyLoginRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.auth.ApiTokenDto
import com.pashaoleynik97.droiddeploy.core.dto.auth.LoginRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.auth.RefreshTokenRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.auth.TokenPairDto
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.RestResponse
import com.pashaoleynik97.droiddeploy.core.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@Tag(
    name = "Authentication",
    description = "User and API key authentication endpoints. All endpoints are public and do not require authentication."
)
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {

    @Operation(
        summary = "User login",
        description = "Authenticate with username and password to receive JWT access and refresh tokens. " +
                "Use the access token for subsequent API calls and the refresh token to obtain new access tokens when they expire."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Login successful. Returns JWT access and refresh tokens.",
                
            ),
            ApiResponse(
                responseCode = "401",
                description = "Authentication failed. Invalid username or password.",
                
            )
        ]
    )
    @PostMapping("/login")
    fun login(
        @RequestBody
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Login credentials (username and password)",
            required = true
        )
        request: LoginRequestDto
    ): ResponseEntity<RestResponse<TokenPairDto>> {
        logger.info { "POST /api/v1/auth/login - Login request received for user: ${request.login}" }

        val tokenPair = authService.login(request)

        logger.info { "Login successful for user: ${request.login}" }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(tokenPair, "Login successful"))
    }

    @Operation(
        summary = "Refresh access token",
        description = "Exchange a valid refresh token for new JWT access and refresh tokens. " +
                "Use this endpoint when the access token expires to obtain a new token pair without requiring the user to log in again."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Token refresh successful. Returns new JWT access and refresh tokens.",
                
            ),
            ApiResponse(
                responseCode = "401",
                description = "Refresh token invalid or expired.",
                
            )
        ]
    )
    @PostMapping("/refresh")
    fun refresh(
        @RequestBody
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Refresh token request",
            required = true
        )
        request: RefreshTokenRequestDto
    ): ResponseEntity<RestResponse<TokenPairDto>> {
        logger.info { "POST /api/v1/auth/refresh - Refresh token request received" }

        val tokenPair = authService.refreshToken(request)

        logger.info { "Refresh token successful" }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(tokenPair, "Token refreshed successfully"))
    }

    @Operation(
        summary = "Authenticate with API key",
        description = "Exchange application-specific API key credentials for an access token. " +
                "This endpoint is designed for CI/CD pipelines and consumer applications. " +
                "The API key must be created by an ADMIN user via the API Keys management endpoints. " +
                "Returns a single access token (no refresh token) with permissions based on the API key's role (CI or CONSUMER)."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "API key authentication successful. Returns access token with CI or CONSUMER role.",
                
            ),
            ApiResponse(
                responseCode = "401",
                description = "API key authentication failed. Invalid API key, inactive key, or key not found.",
                
            )
        ]
    )
    @PostMapping("/apikey")
    fun loginWithApiKey(
        @RequestBody
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "API key credentials (application ID and API key)",
            required = true
        )
        request: ApiKeyLoginRequestDto
    ): ResponseEntity<RestResponse<ApiTokenDto>> {
        logger.info { "POST /api/v1/auth/apikey - API key authentication request received" }

        val apiToken = authService.loginWithApiKey(request)

        logger.info { "API key authentication successful" }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(RestResponse.success(apiToken, "Authentication successful"))
    }
}
