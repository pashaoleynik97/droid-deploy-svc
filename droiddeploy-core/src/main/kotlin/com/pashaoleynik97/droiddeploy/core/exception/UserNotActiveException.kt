package com.pashaoleynik97.droiddeploy.core.exception

/**
 * Exception thrown when user account is not active
 */
class UserNotActiveException(
    message: String = "User account is not active"
) : DroidDeployException(message)
