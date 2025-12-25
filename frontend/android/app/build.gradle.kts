plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.diccionario_hiphop"
    compileSdk = 36  // ✅ ACTUALIZADO de 34 a 36

    defaultConfig {
        applicationId = "com.example.diccionario_hiphop"
        minSdk = 21
        targetSdk = 36  // ✅ ACTUALIZADO de 34 a 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // 🔥 BuildConfig: Variables personalizadas
        buildConfigField("String", "PRODUCTION_URL", "\"https://musictransiator.onrender.com/\"")
        buildConfigField("int", "LOCALHOST_PORT", "8000")
    }

    buildFeatures {
        buildConfig = true  // 🔥 Habilitar BuildConfig
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"  // app.debug vs app
            versionNameSuffix = "-DEBUG"
        }
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // TUS LIBRERÍAS ACTUALES (version catalog)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("io.coil-kt:coil:2.5.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("com.github.yalantis:ucrop:2.2.8")
    implementation(libs.ucrop) // 👈 AÑADIR ESTO

    // DEPENDENCIAS PARA BACKEND
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // TESTS
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}