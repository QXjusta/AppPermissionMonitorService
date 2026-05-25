plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.apppermissionmonitorservice"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.apppermissionmonitorservice"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    buildFeatures {
        aidl = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
