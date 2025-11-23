package com.pashaoleynik97.droiddeploy

import com.pashaoleynik97.droiddeploy.config.UserDefaultsProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(UserDefaultsProperties::class)
class DroiddeployApplication

fun main(args: Array<String>) {
	runApplication<DroiddeployApplication>(*args)
}
