plugins {
    id("com.android.library")
}

android {
    namespace = "mx.utng.snowtrail.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // No specific dependencies needed for shared constants
}
