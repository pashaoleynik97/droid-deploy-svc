package com.pashaoleynik97.droiddeploy

import com.pashaoleynik97.droiddeploy.core.config.UserDefaultsProperties
import com.pashaoleynik97.droiddeploy.security.JwtProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.pashaoleynik97.droiddeploy"])
@EnableConfigurationProperties(UserDefaultsProperties::class, JwtProperties::class)
class DroiddeployApplication

fun main(args: Array<String>) {
	runApplication<DroiddeployApplication>(*args)
}
