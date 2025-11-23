package com.pashaoleynik97.droiddeploy.rest.model.wrapper

data class RestResponse<T> (
    val data: T?,
    val message: String,
    val errors: List<RestError>
) {
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

data class RestError(
    val type: ErrorType,
    val message: String,
    val field: String? = null
) {
    enum class ErrorType {
        VALIDATION,
        AUTHENTICATION,
        AUTHORIZATION,
        NOT_FOUND,
        CONFLICT,
        INTERNAL_ERROR,
        UNKNOWN
    }
}