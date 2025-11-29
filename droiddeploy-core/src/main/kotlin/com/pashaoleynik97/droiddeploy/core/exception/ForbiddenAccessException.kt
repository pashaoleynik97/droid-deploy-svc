package com.pashaoleynik97.droiddeploy.core.exception

/**
 * Exception thrown when a user attempts to access a resource they don't have permission for
 */
class ForbiddenAccessException(
    message: String = "Access denied: insufficient permissions"
) : DroidDeployException(message)
