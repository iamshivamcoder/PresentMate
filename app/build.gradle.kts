plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.appdistribution")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.example.presentmate"
    compileSdk = 35

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
    }

    // Dynamic Versioning Logic
    val baseVersionName = project.findProperty("VERSION_NAME") as? String ?: "1.0"
    val baseVersionCode = (project.findProperty("VERSION_CODE_BASE") as? String)?.toIntOrNull() ?: 1

    val ciRunNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
    val isCiBuild = ciRunNumber != null

    val finalVersionCode = if (isCiBuild) baseVersionCode + ciRunNumber!! else baseVersionCode
    
    val ciCommitSha = System.getenv("GITHUB_SHA")?.take(7)
    val finalVersionName = if (isCiBuild && ciCommitSha != null) {
        "$baseVersionName-$ciCommitSha"
    } else {
        baseVersionName // Local builds keep stable version name
    }

    defaultConfig {
        applicationId = "com.example.presentmate"
        minSdk = 26
        targetSdk = 35
        versionCode = finalVersionCode
        versionName = finalVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("sharedDebug") {
            storeFile = file("presentmate-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            signingConfig = signingConfigs.getByName("sharedDebug") // Use shared keystore to prevent conflict
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            aaptOptions.cruncherEnabled = false

            // Firebase App Distribution
            configure<com.google.firebase.appdistribution.gradle.AppDistributionExtension> {
                artifactType = "APK"
                val credsFile = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
                if (!credsFile.isNullOrBlank()) {
                    serviceCredentialsFile = credsFile
                }

                // Dynamic Release Notes
                if (isCiBuild) {
                    val branchName = System.getenv("CI_BRANCH_NAME") ?: "Unknown Branch"
                    val commitMsg = System.getenv("CI_COMMIT_MESSAGE") ?: "No commit message"
                    val actor = System.getenv("GITHUB_ACTOR") ?: "Automated CI"
                    
                    releaseNotes = """
                        Branch: $branchName
                        Commit: $ciCommitSha
                        Triggered by: $actor
                        
                        Message:
                        $commitMsg
                    """.trimIndent()
                } else {
                    releaseNotes = "Local manual build"
                }
            }
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all",
        )
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    testOptions {
        // Run unit tests in-process without forking a new JVM per test class.
        // Combined with maxParallelForks this is the fastest setup for unit tests.
        unitTests {
            isIncludeAndroidResources = false
            isReturnDefaultValues = true
            all {
                // Run test classes in parallel across half your CPU cores.
                it.maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
                // Reuse the same JVM process for all test classes (no fork overhead).
                it.forkEvery = 0
                // Give each test JVM enough heap.
                it.jvmArgs("-Xmx2048m", "-XX:+UseG1GC")
            }
        }
    }
}

dependencies {
    "baselineProfile"(project(":baselineprofile"))
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.gson)
    
    // Firebase Setup
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-auth")

    // Credential Manager for Google Sign-In
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    
    // Google Drive REST API
    implementation("com.google.api-client:google-api-client-android:2.2.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)

    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    // Encrypted SharedPreferences for secure API key storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Compose dependencies
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.8")
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.57.2")
    ksp("com.google.dagger:hilt-android-compiler:2.57.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")

    // Location Services
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // OpenStreetMap
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // Room
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")

    // Apache POI for .doc export
    implementation("org.apache.poi:poi:5.4.1")
    implementation("org.apache.poi:poi-scratchpad:5.4.1") // Added for HWPF support

    // Gemini AI SDK
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Debug dependencies
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("androidx.navigation:navigation-testing:2.7.6")
}
