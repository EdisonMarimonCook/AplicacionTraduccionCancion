plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.diccionario_hiphop"
    compileSdk = 36  // ✅ ACTUALIZADO de 34 a 36

    defaultConfig {
        applicationId = "com.example.diccionario_hiphop"
        minSdk = 24
        targetSdk = 36  // ✅ ACTUALIZADO de 34 a 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // 🔥 BuildConfig: Variables personalizadas
        buildConfigField("String", "PRODUCTION_URL", "\"https://musictransiator.onrender.com/\"")
        buildConfigField("int", "LOCALHOST_PORT", "8000")

        ndk{
            abiFilters += listOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
        }
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

    // Esto hace lo mismo que "extractNativeLibs=true" pero donde le gusta a Gradle
    packaging {
        jniLibs {
            useLegacyPackaging = true
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
    // TUS LIBRERÍAS ACTUALES
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("io.coil-kt:coil:2.5.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    // Si usas ucrop desde libs o directo, deja solo uno. Asumo que libs.ucrop funciona:
    implementation("com.vanniktech:android-image-cropper:4.7.0")
    // implementation("com.github.yalantis:ucrop:2.2.8") // ❌ Comentado para evitar duplicados

    implementation("de.hdodenhof:circleimageview:3.1.0")

    // 🕸️ Ayuda para leer webs
    implementation ("org.jsoup:jsoup:1.16.1")
    // Para los React skeletons
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    // DEPENDENCIAS PARA BACKEND (Retrofit + Corrutinas unificadas)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // ✅ ÚNICA VERSIÓN de Corrutinas (La más reciente que tenías)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("androidx.recyclerview:recyclerview:1.3.2")

    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")

    // TESTS
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}