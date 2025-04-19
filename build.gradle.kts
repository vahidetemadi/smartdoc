import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("java")
  id("idea")
  id("maven-publish")
  id("org.jetbrains.kotlin.jvm") version "2.1.20"
  id("org.jetbrains.intellij.platform") version "2.5.0"
  //id("org.jetbrains.intellij.platform.migration") version "2.0.1"
}

group = "com.vahid.plugin"
version = "1.0.0"

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  intellijPlatform {
    intellijIdeaCommunity("2025.1")
    bundledPlugins(listOf("com.intellij.java", "Git4Idea"))
    create("IC", "2025.1")

  }
    implementation("com.theokanning.openai-gpt3-java:api:0.18.2")
    implementation("com.theokanning.openai-gpt3-java:client:0.18.2")
    implementation("com.theokanning.openai-gpt3-java:service:0.18.2")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.0")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
  }
//  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
//    kotlinOptions.jvmTarget = "21"
//  }

  kotlin {
    jvmToolchain(21)
    compilerOptions {
      freeCompilerArgs.add("-Xjvm-default=all")
      jvmTarget.set(JvmTarget.JVM_21) // adjust this too
    }
  }


  intellijPlatform {
    buildSearchableOptions = true
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}
