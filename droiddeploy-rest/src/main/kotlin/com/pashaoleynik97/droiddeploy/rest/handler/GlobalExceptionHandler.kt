package com.pashaoleynik97.droiddeploy.rest.handler

import com.pashaoleynik97.droiddeploy.core.exception.DroidDeployException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidCredentialsException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidLoginFormatException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidPasswordException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidRefreshTokenException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidRoleException
import com.pashaoleynik97.droiddeploy.core.exception.LoginAlreadyExistsException
import com.pashaoleynik97.droiddeploy.core.exception.UnauthorizedAccessException
import com.pashaoleynik97.droiddeploy.core.exception.UserNotActiveException
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.RestError
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.RestResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(ex: InvalidCredentialsException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Invalid credentials exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.AUTHENTICATION,
            message = ex.message ?: "Invalid login or password"
        )

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(RestResponse.failure(error, "Authentication failed"))
    }

    @ExceptionHandler(InvalidRefreshTokenException::class)
    fun handleInvalidRefreshToken(ex: InvalidRefreshTokenException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Invalid refresh token exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.VALIDATION,
            message = "Invalid or expired refresh token"
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(RestResponse.failure(error, "Token refresh failed"))
    }

    @ExceptionHandler(UserNotActiveException::class)
    fun handleUserNotActive(ex: UserNotActiveException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "User not active exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.AUTHENTICATION,
            message = ex.message ?: "User account is not active"
        )

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(RestResponse.failure(error, "Authentication failed"))
    }

    @ExceptionHandler(UnauthorizedAccessException::class)
    fun handleUnauthorizedAccess(ex: UnauthorizedAccessException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Unauthorized access exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.AUTHORIZATION,
            message = ex.message ?: "Unauthorized access"
        )

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(RestResponse.failure(error, "Access denied"))
    }

    @ExceptionHandler(LoginAlreadyExistsException::class)
    fun handleLoginAlreadyExists(ex: LoginAlreadyExistsException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Login already exists exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.VALIDATION,
            message = ex.message ?: "Login already exists"
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(RestResponse.failure(error, "User creation failed"))
    }

    @ExceptionHandler(InvalidPasswordException::class)
    fun handleInvalidPassword(ex: InvalidPasswordException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Invalid password exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.VALIDATION,
            message = ex.message ?: "Password doesn't meet security requirements"
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(RestResponse.failure(error, "User creation failed"))
    }

    @ExceptionHandler(InvalidRoleException::class)
    fun handleInvalidRole(ex: InvalidRoleException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Invalid role exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.VALIDATION,
            message = ex.message ?: "Invalid role"
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(RestResponse.failure(error, "User creation failed"))
    }

    @ExceptionHandler(InvalidLoginFormatException::class)
    fun handleInvalidLoginFormat(ex: InvalidLoginFormatException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Invalid login format exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.VALIDATION,
            message = ex.message ?: "Invalid login format"
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(RestResponse.failure(error, "User creation failed"))
    }

    @ExceptionHandler(DroidDeployException::class)
    fun handleDroidDeployException(ex: DroidDeployException): ResponseEntity<RestResponse<Nothing>> {
        logger.error(ex) { "DroidDeploy exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.INTERNAL_ERROR,
            message = ex.message ?: "An error occurred"
        )

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(RestResponse.failure(error, "Operation failed"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<RestResponse<Nothing>> {
        logger.error(ex) { "Unexpected exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.INTERNAL_ERROR,
            message = "An unexpected error occurred"
        )

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(RestResponse.failure(error, "Internal server error"))
    }
}
