package com.pashaoleynik97.droiddeploy.core.exception

class BundleIdAlreadyExistsException(
    bundleId: String
) : DroidDeployException("Application with bundle id '$bundleId' already exists")
