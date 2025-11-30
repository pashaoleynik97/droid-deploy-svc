package com.pashaoleynik97.droiddeploy.core.exception

class InvalidVersionCodeException(
    versionCode: Int,
    maxVersionCode: Int
) : DroidDeployException(
    "Version code '$versionCode' must be greater than the current maximum version code '$maxVersionCode'"
)
