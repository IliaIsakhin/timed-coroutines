import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.7.10"
}

group = "ilia.isakhin"
version = "0.0.1-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.aspectj:aspectjweaver:1.8.+")

	implementation("io.micrometer:micrometer-core:latest.integration")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:latest.release")

	testImplementation("org.junit.jupiter:junit-jupiter:latest.release")
	testImplementation("org.springframework:spring-aop:latest.release")
	testImplementation("org.assertj:assertj-core:latest.release")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
