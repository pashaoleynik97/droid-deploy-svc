package com.pashaoleynik97.droiddeploy.core.exception

class InvalidRefreshTokenException(
    message: String = "Invalid or expired refresh token"
) : DroidDeployException(message)
