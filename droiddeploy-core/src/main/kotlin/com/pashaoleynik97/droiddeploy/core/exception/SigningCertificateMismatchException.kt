package com.pashaoleynik97.droiddeploy.core.exception

class SigningCertificateMismatchException(
    applicationId: String,
    expectedSha256: String,
    actualSha256: String
) : DroidDeployException(
    "Signing certificate mismatch for application '$applicationId'. Expected: $expectedSha256, but got: $actualSha256"
)
