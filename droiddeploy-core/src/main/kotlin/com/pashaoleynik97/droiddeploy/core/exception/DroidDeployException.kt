package com.pashaoleynik97.droiddeploy.core.exception

/**
 * Base exception for all DroidDeploy domain exceptions
 */
abstract class DroidDeployException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
