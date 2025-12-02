package com.pashaoleynik97.droiddeploy.core.exception

import java.util.UUID

/**
 * Exception thrown when an application version is not found
 */
class ApplicationVersionNotFoundException : DroidDeployException {
    constructor(applicationId: UUID, versionCode: Long) : super(
        "Application version with applicationId='$applicationId' and versionCode='$versionCode' not found"
    )

    constructor(applicationId: UUID) : super(
        "No versions found for application with applicationId='$applicationId'"
    )
}
