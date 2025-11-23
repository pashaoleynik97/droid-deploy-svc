package com.pashaoleynik97.droiddeploy.core.exception

/**
 * Exception thrown when login credentials are invalid
 */
class InvalidCredentialsException(
    message: String = "Invalid login or password"
) : DroidDeployException(message)
