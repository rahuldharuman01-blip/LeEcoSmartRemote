plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android { namespace = "com.rahul.leecoremote"; compileSdk = 36
    defaultConfig { applicationId = "com.rahul.leecoremote"; minSdk = 23; targetSdk = 36; versionCode = 2; versionName = "2.0-liquid-glass" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.flyfishxu:kadb:2.1.4")
}
