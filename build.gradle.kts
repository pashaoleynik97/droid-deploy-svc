plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21" apply false
	kotlin("plugin.jpa") version "2.2.21" apply false
	id("org.springframework.boot") version "4.0.0" apply false
	id("io.spring.dependency-management") version "1.1.7" apply false
}

group = "com.pashaoleynik97"
version = "0.0.1-SNAPSHOT"

repositories {
	mavenCentral()
}

subprojects {
	group = "com.pashaoleynik97"
	version = "0.0.1-SNAPSHOT"

	repositories {
		mavenCentral()
	}

	tasks.withType<Test> {
		useJUnitPlatform()
	}

	tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
		compilerOptions {
			freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
		}
	}
}

configure(subprojects) {
	apply(plugin = "org.jetbrains.kotlin.jvm")

	java {
		toolchain {
			languageVersion = JavaLanguageVersion.of(21)
		}
	}

	dependencies {
		testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
		testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	}
}
