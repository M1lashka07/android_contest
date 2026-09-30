plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.10"
    `maven-publish`
}
group = "ru.professionals"
version = "1.0.0"
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
dependencies {
    compileOnly("org.json:json:20240303")
    testImplementation("org.json:json:20240303")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
publishing { publications { create<MavenPublication>("network") { from(components["java"]) } } }
