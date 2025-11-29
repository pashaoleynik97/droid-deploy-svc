package com.pashaoleynik97.droiddeploy.config

import com.pashaoleynik97.droiddeploy.core.config.UserDefaultsProperties
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.core.service.UserService
import mu.KotlinLogging
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class SuperAdminInitializer(
    private val userService: UserService,
    private val userDefaultsProperties: UserDefaultsProperties
) : CommandLineRunner {

    override fun run(vararg args: String) {
        val superAdminLogin = userDefaultsProperties.superAdminLogin

        if (!userService.userExists(superAdminLogin)) {
            logger.info { "Super admin user not found. Creating user with login: $superAdminLogin" }

            val superAdmin = userService.createUser(
                login = superAdminLogin,
                password = userDefaultsProperties.superAdminPassword,
                role = UserRole.ADMIN
            )

            logger.info { "Super admin user created successfully with ID: ${superAdmin.id}" }
        } else {
            logger.info { "Super admin user already exists with login: $superAdminLogin" }
        }
    }
}
