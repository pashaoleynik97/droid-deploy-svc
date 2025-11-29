package com.pashaoleynik97.droiddeploy.core.exception

import java.util.UUID

/**
 * Exception thrown when a user is not found by ID
 */
class UserNotFoundException(
    userId: UUID
) : DroidDeployException("User with ID '$userId' not found")
