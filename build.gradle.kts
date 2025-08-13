plugins {
      kotlin("jvm") version "2.2.0"
      kotlin("plugin.spring") version "2.2.0"
      id("org.springframework.boot") version "3.5.4"
      id("io.spring.dependency-management") version "1.1.7"
      kotlin("plugin.jpa") version "2.2.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
      toolchain {
            languageVersion = JavaLanguageVersion.of(21)
      }
}

repositories {
      mavenCentral()
}

dependencies {
      implementation("org.springframework.boot:spring-boot-starter-data-jpa")
      implementation("org.springframework.boot:spring-boot-starter-security")
      implementation("org.springframework.boot:spring-boot-starter-web")
      implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
      implementation("org.mapstruct:mapstruct:1.6.3")
      implementation("org.springframework.boot:spring-boot-starter-mail:3.5.4")
      implementation("org.springframework:spring-context-support:6.2.9")
      implementation("io.github.cdimascio:dotenv-java:3.2.0")
      implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
      implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
      implementation("com.fasterxml.jackson.core:jackson-core:2.19.2")
      implementation("com.squareup.okhttp3:okhttp:5.1.0")
      implementation("org.jetbrains.kotlin:kotlin-reflect")
      developmentOnly("org.springframework.boot:spring-boot-devtools")
      runtimeOnly("org.postgresql:postgresql")
      testImplementation("org.springframework.boot:spring-boot-starter-test")
      testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
      testImplementation("org.springframework.security:spring-security-test")
      testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
      compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict")
      }
}

allOpen {
      annotation("jakarta.persistence.Entity")
      annotation("jakarta.persistence.MappedSuperclass")
      annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
      useJUnitPlatform()
}
