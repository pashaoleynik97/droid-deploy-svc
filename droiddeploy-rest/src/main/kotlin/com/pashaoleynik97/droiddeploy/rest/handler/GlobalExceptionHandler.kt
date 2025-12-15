package com.pashaoleynik97.droiddeploy.rest.handler

import com.pashaoleynik97.droiddeploy.core.exception.ApiKeyExpiredException
import com.pashaoleynik97.droiddeploy.core.exception.ApiKeyNotFoundException
import com.pashaoleynik97.droiddeploy.core.exception.ApiKeyRevokedException
import com.pashaoleynik97.droiddeploy.core.exception.ApkNotFoundException
import com.pashaoleynik97.droiddeploy.core.exception.ApkStorageException
import com.pashaoleynik97.droiddeploy.core.exception.ApplicationNotFoundException
import com.pashaoleynik97.droiddeploy.core.exception.ApplicationVersionAlreadyExistsException
import com.pashaoleynik97.droiddeploy.core.exception.ApplicationVersionNotFoundException
import com.pashaoleynik97.droiddeploy.core.exception.BundleIdAlreadyExistsException
import com.pashaoleynik97.droiddeploy.core.exception.DroidDeployException
import com.pashaoleynik97.droiddeploy.core.exception.ForbiddenAccessException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidApiKeyException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidApiKeyRoleException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidApplicationNameException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidBundleIdException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidCredentialsException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidLoginFormatException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidPasswordException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidRefreshTokenException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidRoleException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidUserTypeException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidVersionCodeException
import com.pashaoleynik97.droiddeploy.core.exception.LoginAlreadyExistsException
import com.pashaoleynik97.droiddeploy.core.exception.SelfModificationNotAllowedException
import com.pashaoleynik97.droiddeploy.core.exception.SigningCertificateMismatchException
import com.pashaoleynik97.droiddeploy.core.exception.SuperAdminProtectionException
import com.pashaoleynik97.droiddeploy.core.exception.UnauthorizedAccessException
import com.pashaoleynik97.droiddeploy.core.exception.UserNotActiveException
import com.pashaoleynik97.droiddeploy.core.exception.UserNotFoundException
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.RestError
import com.pashaoleynik97.droiddeploy.rest.model.wrapper.RestResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
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

    @ExceptionHandler(InvalidUserTypeException::class)
    fun handleInvalidUserType(ex: InvalidUserTypeException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Invalid user type exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.VALIDATION,
            message = ex.message ?: "Invalid user type for this operation"
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(RestResponse.failure(error, "Operation failed"))
    }

    @ExceptionHandler(SelfModificationNotAllowedException::class)
    fun handleSelfModificationNotAllowed(ex: SelfModificationNotAllowedException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Self modification not allowed exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.AUTHORIZATION,
            message = ex.message ?: "You cannot modify your own account"
        )

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(RestResponse.failure(error, "Operation failed"))
    }

    @ExceptionHandler(SuperAdminProtectionException::class)
    fun handleSuperAdminProtection(ex: SuperAdminProtectionException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Super admin protection exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.AUTHORIZATION,
            message = ex.message ?: "Super admin account cannot be modified"
        )

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(RestResponse.failure(error, "Operation failed"))
    }

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(ex: UserNotFoundException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "User not found exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.NOT_FOUND,
            message = ex.message ?: "User not found"
        )

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(RestResponse.failure(error, "User not found"))
    }

    @ExceptionHandler(ApplicationNotFoundException::class)
    fun handleApplicationNotFound(ex: ApplicationNotFoundException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Application not found exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.NOT_FOUND,
            message = ex.message ?: "Application not found"
        )

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(RestResponse.failure(error, "Application not found"))
    }

    @ExceptionHandler(ApplicationVersionNotFoundException::class)
    fun handleApplicationVersionNotFound(ex: ApplicationVersionNotFoundException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Application version not found exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.NOT_FOUND,
            message = ex.message ?: "Application version not found"
        )

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(RestResponse.failure(error, "Application version not found"))
    }

    @ExceptionHandler(ApkNotFoundException::class)
    fun handleApkNotFound(ex: ApkNotFoundException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "APK not found exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.NOT_FOUND,
            message = ex.message ?: "APK file not found"
        )

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(RestResponse.failure(error, "APK file not found"))
    }

    @ExceptionHandler(ApkStorageException::class)
    fun handleApkStorageException(ex: ApkStorageException): ResponseEntity<RestResponse<Nothing>> {
        logger.error(ex) { "APK storage exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.INTERNAL_ERROR,
            message = ex.message ?: "Failed to access APK storage"
        )

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(RestResponse.failure(error, "APK storage operation failed"))
    }

    @ExceptionHandler(ForbiddenAccessException::class)
    fun handleForbiddenAccess(ex: ForbiddenAccessException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Forbidden access exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.AUTHORIZATION,
            message = ex.message ?: "Access denied"
        )

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(RestResponse.failure(error, "Access denied"))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Access denied exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.AUTHORIZATION,
            message = "Access denied: insufficient permissions"
        )

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(RestResponse.failure(error, "Access denied"))
    }

    @ExceptionHandler(InvalidApplicationNameException::class)
    fun handleInvalidApplicationName(ex: InvalidApplicationNameException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Invalid application name exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.VALIDATION,
            message = ex.message ?: "Application name is invalid"
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(RestResponse.failure(error, "Application creation failed"))
    }

    @ExceptionHandler(InvalidBundleIdException::class)
    fun handleInvalidBundleId(ex: InvalidBundleIdException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Invalid bundle id exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.VALIDATION,
            message = ex.message ?: "Bundle id is invalid"
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(RestResponse.failure(error, "Application creation failed"))
    }

    @ExceptionHandler(BundleIdAlreadyExistsException::class)
    fun handleBundleIdAlreadyExists(ex: BundleIdAlreadyExistsException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Bundle id already exists exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.VALIDATION,
            message = ex.message ?: "Application with this bundle id already exists"
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(RestResponse.failure(error, "Application creation failed"))
    }

    @ExceptionHandler(SigningCertificateMismatchException::class)
    fun handleSigningCertificateMismatch(ex: SigningCertificateMismatchException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Signing certificate mismatch exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.VALIDATION,
            message = ex.message ?: "Signing certificate does not match the expected certificate"
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(RestResponse.failure(error, "Version upload failed"))
    }

    @ExceptionHandler(ApplicationVersionAlreadyExistsException::class)
    fun handleApplicationVersionAlreadyExists(ex: ApplicationVersionAlreadyExistsException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Application version already exists exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.VALIDATION,
            message = ex.message ?: "Application version already exists"
        )

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(RestResponse.failure(error, "Version upload failed"))
    }

    @ExceptionHandler(InvalidVersionCodeException::class)
    fun handleInvalidVersionCode(ex: InvalidVersionCodeException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Invalid version code exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.VALIDATION,
            message = ex.message ?: "Version code must be greater than existing versions"
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(RestResponse.failure(error, "Version upload failed"))
    }

    @ExceptionHandler(ApiKeyNotFoundException::class)
    fun handleApiKeyNotFound(ex: ApiKeyNotFoundException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "API key not found exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.NOT_FOUND,
            message = ex.message ?: "API key not found"
        )

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(RestResponse.failure(error, "API key not found"))
    }

    @ExceptionHandler(InvalidApiKeyException::class)
    fun handleInvalidApiKey(ex: InvalidApiKeyException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Invalid API key exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.AUTHENTICATION,
            message = ex.message ?: "API key not found or invalid"
        )

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(RestResponse.failure(error, "Authentication failed"))
    }

    @ExceptionHandler(ApiKeyRevokedException::class)
    fun handleApiKeyRevoked(ex: ApiKeyRevokedException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "API key revoked exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.AUTHENTICATION,
            message = ex.message ?: "API key has been revoked"
        )

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(RestResponse.failure(error, "Authentication failed"))
    }

    @ExceptionHandler(ApiKeyExpiredException::class)
    fun handleApiKeyExpired(ex: ApiKeyExpiredException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "API key expired exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.AUTHENTICATION,
            message = ex.message ?: "API key has expired"
        )

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(RestResponse.failure(error, "Authentication failed"))
    }

    @ExceptionHandler(InvalidApiKeyRoleException::class)
    fun handleInvalidApiKeyRole(ex: InvalidApiKeyRoleException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Invalid API key role exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.VALIDATION,
            message = ex.message ?: "Invalid API key role"
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(RestResponse.failure(error, "API key creation failed"))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<RestResponse<Nothing>> {
        logger.warn { "Illegal argument exception: ${ex.message}" }

        val error = RestError(
            type = RestError.ErrorType.VALIDATION,
            message = ex.message ?: "Invalid parameter"
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(RestResponse.failure(error, "Validation failed"))
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