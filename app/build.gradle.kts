plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("maven-publish")
}

android {
    namespace = "com.example.hourlychime"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.hourlychime"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.5.0"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    lint {
        abortOnError = false
        baseline = file("lint-baseline.xml")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Standard Android & Kotlin libraries
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("com.google.android.gms:play-services-wearable:20.0.1")
    implementation("androidx.percentlayout:percentlayout:1.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // Wear Compose Libraries for the UI
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.wear.compose:compose-material:1.6.2")
    implementation("androidx.wear.compose:compose-foundation:1.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    testImplementation("junit:junit:4.13.2")
}

// THIS IS THE CORRECTED PUBLISHING BLOCK
afterEvaluate {
    publishing {
        publications {
            // Create a publication for our debug APK
            create<MavenPublication>("debug") {
                groupId = "com.github.returnofthelambda"
                artifactId = "hourly-chime" 
                version = project.findProperty("appVersion")?.toString() ?: android.defaultConfig.versionName

                // Tell Gradle which file to publish and which task creates it.
                artifact(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk")) {
                    builtBy(tasks.named("assembleDebug"))
                }
            }
        }
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY")}")
                credentials {
                    username = System.getenv("USERNAME")
                    password = System.getenv("TOKEN")
                }
            }
        }
    }
}
