package com.pashaoleynik97.droiddeploy.core.exception

/**
 * Exception thrown when password doesn't meet security requirements
 */
class InvalidPasswordException(
    message: String = "Password must be at least 10 characters and contain uppercase, lowercase, and digit"
) : DroidDeployException(message)
