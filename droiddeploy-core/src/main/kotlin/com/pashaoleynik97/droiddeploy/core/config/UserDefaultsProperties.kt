package com.pashaoleynik97.droiddeploy.core.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "user-defaults")
data class UserDefaultsProperties(
    val superAdminLogin: String,
    val superAdminPassword: String
)
