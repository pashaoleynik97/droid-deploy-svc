package com.pashaoleynik97.droiddeploy.core.exception

/**
 * Exception thrown when login format doesn't meet requirements
 */
class InvalidLoginFormatException(
    message: String = "Login must be 3-20 characters and contain only letters, numbers, underscores, and dashes"
) : DroidDeployException(message)
