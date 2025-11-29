package com.pashaoleynik97.droiddeploy.core.exception

import com.pashaoleynik97.droiddeploy.core.domain.UserRole

/**
 * Exception thrown when attempting password operations on non-human users
 */
class InvalidUserTypeException(
    role: UserRole
) : DroidDeployException("User with role $role cannot have password updated. Only ADMIN users can have passwords.")
