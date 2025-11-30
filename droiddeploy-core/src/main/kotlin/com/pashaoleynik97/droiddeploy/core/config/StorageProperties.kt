package com.pashaoleynik97.droiddeploy.core.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "droiddeploy.storage")
data class StorageProperties(
    var root: String = "/var/lib/droiddeploy/apks"
)