import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  id("java")
  id("idea")
  id("maven-publish")
  id("org.jetbrains.kotlin.jvm") version "2.1.20"
  id("org.jetbrains.intellij.platform") version "2.5.0"
  //id("org.jetbrains.intellij.platform.migration") version "2.0.5"
}

group = "com.vahid.plugin"
version = "1.0.0"

repositories {
  mavenCentral()
  maven (url = "https://jitpack.io")
  maven (url = "https://www.jetbrains.com/intellij-repository/releases")
  maven (url = "https://www.jetbrains.com/intellij-repository/snapshots")
  maven (url = "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  intellijPlatform {
    intellijIdeaCommunity("2025.1")
    bundledPlugins(listOf("com.intellij.java", "Git4Idea"))
    create("IC", "2025.1")
    testFramework(TestFrameworkType.Plugin.Java)
  }
    implementation("com.theokanning.openai-gpt3-java:api:0.18.2")
    implementation("com.theokanning.openai-gpt3-java:client:0.18.2")
    implementation("com.theokanning.openai-gpt3-java:service:0.18.2")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.0")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("dev.langchain4j:langchain4j:1.0.0-beta3")
    implementation("dev.langchain4j:langchain4j-open-ai:1.0.0-beta3")
    implementation("dev.langchain4j:langchain4j-ollama:1.0.0-beta3")

    // For testing
    testImplementation("org.assertj:assertj-core:3.27.2")
    testImplementation("org.mockito:mockito-core:5.17.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.17.0")
    testImplementation("org.junit.platform:junit-platform-launcher:1.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("com.intellij.remoterobot:remote-robot:0.11.16")
    testImplementation("com.intellij.remoterobot:remote-fixtures:0.11.16")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

tasks {
  withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
    options.compilerArgs.add("--enable-preview")
  }

  kotlin {
    jvmToolchain(21)
    compilerOptions {
      freeCompilerArgs.add("-Xjvm-default=all")
      jvmTarget.set(JvmTarget.JVM_21)
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

  test {
    useJUnitPlatform()
    jvmArgs = listOf("--enable-preview")
  }

  runIde {
    jvmArgs = listOf("--enable-preview")
  }
}