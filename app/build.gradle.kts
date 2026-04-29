plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ilkokuluncu.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ilkokuluncu.app"
        minSdk = 28          // Android 9.0 (Pie) – cihazların %97'si
        targetSdk = 35
        versionCode = 4      // Play Store'a her yüklemede artır
        versionName = "1.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // ── İmzalama (release için) ───────────────────────────────────────────────
    // keystore dosyasını proje köküne koy, aşağıdaki değerleri doldur
    // Güvenlik için bu bilgileri gradle.properties veya environment variable'a taşı
    signingConfigs {
        // "release" signing config'i aşağıdaki komutla oluştur:
        // keytool -genkey -v -keystore ilkokuluncu.jks -keyalg RSA
        //         -keysize 2048 -validity 10000 -alias ilkokuluncu
        //
        // Sonra gradle.properties dosyasına ekle:
        //   KEYSTORE_PATH=../ilkokuluncu.jks
        //   KEYSTORE_PASSWORD=şifren
        //   KEY_ALIAS=ilkokuluncu
        //   KEY_PASSWORD=şifren
        //
        // Ve aşağıdaki bloğun yorumunu kaldır:
        create("release") {
            storeFile     = file(project.property("KEYSTORE_PATH") as String)
            storePassword = project.property("KEYSTORE_PASSWORD") as String
            keyAlias      = project.property("KEY_ALIAS") as String
            keyPassword   = project.property("KEY_PASSWORD") as String
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
            isDebuggable        = true
        }
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose      = true
        buildConfig  = true   // BuildConfig.DEBUG erişimi için
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // Play Store için bundle tercih edilir
    bundle {
        language { enableSplit = true }
        density  { enableSplit = true }
        abi      { enableSplit = true }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Accompanist
    implementation("com.google.accompanist:accompanist-pager:0.32.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.32.0")

    // Coil (resim yükleme)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ── Reklam SDK'ları ───────────────────────────────────────────────────────
    // Google Mobile Ads (AdMob)
    implementation("com.google.android.gms:play-services-ads:23.3.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
