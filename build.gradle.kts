import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  id("java")
  id("idea")
  id("maven-publish")
  id("org.jetbrains.kotlin.jvm") version "2.1.20"
  id("org.jetbrains.intellij.platform") version "2.5.0"
  id("com.github.johnrengelman.shadow") version "8.1.1"
  //id("org.jetbrains.intellij.platform.migration") version "2.0.5"
}

group = "com.vahid.plugin"
version = "1.0.0"

repositories {
  mavenCentral()
  maven (url = "https://jitpack.io")
  maven (url = "https://mvnrepository.com/artifact/com.jetbrains.intellij.platform/test-framework")
  maven (url = "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
  maven (url = "https://www.jetbrains.com/intellij-repository/releases")
  maven (url = "https://www.jetbrains.com/intellij-repository/snapshots")
  maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
  maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
  maven("https://cache-redirector.jetbrains.com/repo1.maven.org/maven2")
  maven("https://packages.jetbrains.team/maven/p/kpm/public")
  maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
  maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
  maven("https://packages.jetbrains.team/maven/p/kpm/public")
  maven("https://www.jetbrains.com/intellij-repository/releases")
  maven("https://cache-redirector.jetbrains.com/www.jetbrains.com/intellij-repository/releases")
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  intellijPlatform {
    intellijIdeaCommunity("2025.1")
    bundledPlugins(listOf("com.intellij.java", "Git4Idea"))
    create("IC", "2025.1")
    testFramework(TestFrameworkType.Platform)
    testFramework(TestFrameworkType.Plugin.Java)
  }
    implementation(kotlin("stdlib"))
    implementation("com.theokanning.openai-gpt3-java:api:0.18.2")
    implementation("com.theokanning.openai-gpt3-java:client:0.18.2")
    implementation("com.theokanning.openai-gpt3-java:service:0.18.2")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    //implementation("com.fasterxml.jackson.core:jackson-databind:2.15.0")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("dev.langchain4j:langchain4j:1.0.0-beta3")
    implementation("dev.langchain4j:langchain4j-open-ai:1.0.0-beta3")
    implementation("dev.langchain4j:langchain4j-ollama:1.0.0-beta3")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation("org.springframework:spring-webflux:6.0.10") {
      exclude(group = "com.fasterxml.jackson.core")
      exclude(group = "com.fasterxml.jackson.module")
      exclude(group = "com.fasterxml.jackson.datatype")
    }
    implementation("io.projectreactor:reactor-core:3.5.7")
    implementation("io.projectreactor.netty:reactor-netty-http:1.1.9")

    // Bring your own complete Jackson stack
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")

    // For testing
    testImplementation("org.mockito:mockito-core:5.17.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.17.0")
    testImplementation("org.junit.platform:junit-platform-launcher:1.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("com.intellij.remoterobot:remote-robot:0.11.18")
    testImplementation("com.intellij.remoterobot:remote-fixtures:0.11.18")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.9.2")
//    testImplementation("com.jetbrains.intellij.platform:test-framework:251.23774.444") {
//      exclude(group = "ai.grazie.spell.*")
//      exclude(group = "ai.grazie.nlp.*")
//    }

//  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
//  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
//  testImplementation("org.mockito:mockito-core:5.6.0")
//  testImplementation("org.mockito:mockito-junit-jupiter:5.6.0")
//  testImplementation("org.assertj:assertj-core:3.27.2")

}
// TODO force all kotlin deps to match version 2.1.20
//configurations.all {
//  resolutionStrategy.eachDependency {
//    if (requested.group == "org.jetbrains.kotlin") {
//      useVersion("2.1.20")
//    }
//  }
//}

tasks {

  withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
    options.compilerArgs.add("--enable-preview")
    options.compilerArgs.add("-Xlint:deprecation")
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

//  jar {
//    archiveFileName.set("${project.name}-${project.version}.jar")
//    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
//    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//  }

  shadowJar {
    archiveClassifier.set("")
  }

//  jar {
//    enabled = false
//  }

  prepareSandbox {
    dependsOn(shadowJar)
  }

  prepareTestSandbox {
    dependsOn(shadowJar)
  }

  build {
    dependsOn(shadowJar)
  }

  buildPlugin {
    dependsOn(shadowJar, test)
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
//    doFirst {
//      if (":buildPlugin" in gradle.startParameter.taskNames) {
//        outputs.upToDateWhen { false }
//      }
//    }
    outputs.upToDateWhen { false }
    useJUnitPlatform()
    jvmArgs = listOf("--enable-preview")
  }

  runIde {
    jvmArgs = listOf("--enable-preview", "-ea")
  }
}