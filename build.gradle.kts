plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21" apply false
	kotlin("plugin.jpa") version "2.2.21" apply false
	id("org.springframework.boot") version "4.0.0" apply false
	id("io.spring.dependency-management") version "1.1.7" apply false
}

// Dynamic versioning: can be overridden via -Pversion=X.Y.Z
// Note: Gradle's built-in "version" property takes precedence, so we use a custom property name
val projectVersion = findProperty("revision")?.toString() ?: "0.0.0-SNAPSHOT"

group = "com.pashaoleynik97"
version = projectVersion

repositories {
	mavenCentral()
}

subprojects {
	group = "com.pashaoleynik97"
	version = rootProject.version  // Inherit version from root project

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
