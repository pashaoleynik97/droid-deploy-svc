package com.pashaoleynik97.droiddeploy.core.exception

/**
 * Exception thrown when user attempts unauthorized access
 */
class UnauthorizedAccessException(
    message: String = "Unauthorized access"
) : DroidDeployException(message)
