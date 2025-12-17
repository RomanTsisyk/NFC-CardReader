plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "io.github.romantsisyk.nfccardreader"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.romantsisyk.nfccardreader"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Room schema export
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        
        // Add this for record desugaring
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Fix for duplicate META-INF files
            excludes += "META-INF/gradle/incremental.annotation.processors"
        }
    }
    // Enable test options
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            
            // Add VM args for Mockito with Java 21
            all {
                it.jvmArgs("-Dnet.bytebuddy.experimental=true")
            }
        }
    }
}

dependencies {
    // Add this for record desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)  // Changed from implementation to ksp

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Adding Material Icons Extended for additional icons
    implementation(libs.androidx.material.icons.extended)

    // Room database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Navigation Compose
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    
    // Base testing dependencies
    testImplementation(libs.junit)
    
    // Mockito for mocking - using older versions known to be stable
    testImplementation("org.mockito:mockito-core:3.12.4")
    testImplementation("org.mockito:mockito-inline:3.12.4") 
    
    // Coroutines testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    
    // AndroidX test dependencies
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test:runner:1.5.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0") // For InstantTaskExecutorRule
    
    // Robolectric for Android framework simulation in unit tests
    testImplementation("org.robolectric:robolectric:4.9")
    
    // Hilt testing
    testImplementation("com.google.dagger:hilt-android-testing:${libs.versions.hiltAndroid.get()}")
    kspTest("com.google.dagger:hilt-android-compiler:${libs.versions.hiltAndroid.get()}")

    // Fix for Android instrumented tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")
    
    // Hilt testing for Android tests
    androidTestImplementation("com.google.dagger:hilt-android-testing:${libs.versions.hiltAndroid.get()}")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:${libs.versions.hiltAndroid.get()}")
    
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}