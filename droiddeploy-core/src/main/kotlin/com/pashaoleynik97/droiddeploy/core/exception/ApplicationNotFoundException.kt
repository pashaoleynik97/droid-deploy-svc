package com.pashaoleynik97.droiddeploy.core.exception

import java.util.UUID

/**
 * Exception thrown when an application is not found by ID
 */
class ApplicationNotFoundException(
    applicationId: UUID
) : DroidDeployException("Application with ID '$applicationId' not found")
