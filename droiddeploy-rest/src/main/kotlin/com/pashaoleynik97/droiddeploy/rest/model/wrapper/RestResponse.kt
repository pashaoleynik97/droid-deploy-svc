package com.pashaoleynik97.droiddeploy.rest.model.wrapper

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Standard response wrapper for all API endpoints",
    example = """{"data": {...}, "message": "Operation completed successfully", "errors": [], "success": true}"""
)
data class RestResponse<T> (
    @Schema(
        description = "Response payload data. Contains the requested resource or result. Null on error.",
        nullable = true
    )
    val data: T?,

    @Schema(
        description = "Human-readable message describing the operation result",
        example = "Operation completed successfully"
    )
    val message: String,

    @Schema(
        description = "List of errors that occurred during the operation. Empty on success.",
        example = "[]"
    )
    val errors: List<RestError>
) {

    @get:Schema(
        description = "Indicates whether the operation was successful. True if errors list is empty.",
        example = "true"
    )
    @Suppress("unused")
    val success: Boolean
        get() = errors.isEmpty()

    companion object {
        fun <T> success(data: T?, message: String = "Success"): RestResponse<T> {
            return RestResponse(data, message, emptyList())
        }

        fun <T> failure(errors: List<RestError>, message: String = "Failure"): RestResponse<T> {
            return RestResponse(null, message, errors)
        }

        fun <T> failure(error: RestError, message: String = "Failure"): RestResponse<T> {
            return RestResponse(null, message, listOf(error))
        }
    }
}

@Schema(description = "Error information for failed operations")
data class RestError(
    @Schema(
        description = "Error type classification",
        implementation = ErrorType::class,
        example = "VALIDATION"
    )
    val type: ErrorType,

    @Schema(
        description = "Detailed error message",
        example = "Invalid input provided"
    )
    val message: String,

    @Schema(
        description = "Field name for validation errors. Null for non-field errors.",
        nullable = true,
        example = "email"
    )
    val field: String? = null
) {
    @Schema(description = "Error type classification")
    enum class ErrorType {
        @Schema(description = "Input validation failed")
        VALIDATION,

        @Schema(description = "Authentication credentials invalid or expired")
        AUTHENTICATION,

        @Schema(description = "Insufficient permissions for this operation")
        AUTHORIZATION,

        @Schema(description = "Requested resource not found")
        NOT_FOUND,

        @Schema(description = "Resource conflict (e.g., duplicate entry)")
        CONFLICT,

        @Schema(description = "Internal server error")
        INTERNAL_ERROR,

        @Schema(description = "Unknown or unclassified error")
        UNKNOWN
    }
}