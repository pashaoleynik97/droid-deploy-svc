package com.pashaoleynik97.droiddeploy.core.exception

class ApplicationVersionAlreadyExistsException(
    applicationId: String,
    versionCode: Int
) : DroidDeployException(
    "Application version with version code '$versionCode' already exists for application '$applicationId'"
)
