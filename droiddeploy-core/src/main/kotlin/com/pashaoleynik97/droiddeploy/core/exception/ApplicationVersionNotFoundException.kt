package com.pashaoleynik97.droiddeploy.core.exception

import java.util.UUID

/**
 * Exception thrown when an application version is not found
 */
class ApplicationVersionNotFoundException(
    applicationId: UUID,
    versionCode: Long
) : DroidDeployException("Application version with applicationId='$applicationId' and versionCode='$versionCode' not found")
