plugins { id("com.android.application"); id("org.jetbrains.kotlin.plugin.compose") }
android {
    namespace = "ru.professionals.storybook"
    compileSdk { version = release(37) { minorApiLevel = 0 } }
    defaultConfig { applicationId = "ru.professionals.storybook"; minSdk = 34; targetSdk = 37; versionCode = 1; versionName = "1.0.0" }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies { implementation("ru.professionals:uikit:1.0.0"); implementation("androidx.activity:activity-compose:1.10.1") }
