plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kotlin.multiplatform)
}

kotlin {
    androidLibrary {
        namespace = "com.sans.finance.shared"
        compileSdk = 37
        minSdk = 36
    }
    
    jvm() // For Ktor server

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation("javax.inject:javax.inject:1")
            implementation(libs.androidx.room.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
