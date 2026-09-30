plugins {
    id("com.android.library") version "9.4.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    `maven-publish`
}
group = "ru.professionals"
version = "1.0.0"
android {
    namespace = "ru.professionals.uikit"
    compileSdk { version = release(37) { minorApiLevel = 0 } }
    defaultConfig { minSdk = 34 }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    publishing { singleVariant("release") { withSourcesJar() } }
}
dependencies {
    api(platform("androidx.compose:compose-bom:2026.02.01"))
    api("androidx.compose.material3:material3")
    api("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")
}
afterEvaluate { publishing { publications { register<MavenPublication>("release") { from(components["release"]) } } } }
