plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	id("org.jetbrains.kotlin.plugin.compose")
}

android {
	namespace = "com.example.hourlychime"
		compileSdk = 34

		defaultConfig {
			applicationId = "com.example.hourlychime"
				minSdk = 30
				targetSdk = 34
				versionCode = 1
				versionName = "1.0"
				vectorDrawables {
					useSupportLibrary = true
				}
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
		sourceCompatibility = JavaVersion.VERSION_1_8
			targetCompatibility = JavaVersion.VERSION_1_8
	}
	kotlinOptions {
		jvmTarget = "1.8"
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
		}
	}
}

dependencies {
	// Standard Android & Kotlin libraries
	implementation("androidx.core:core-ktx:1.13.1")
		implementation("com.google.android.gms:play-services-wearable:18.2.0")
		implementation("androidx.percentlayout:percentlayout:1.0.0")
		implementation("androidx.legacy:legacy-support-v4:1.0.0")
		implementation("androidx.recyclerview:recyclerview:1.3.2")

		// Wear Compose Libraries for the UI
		implementation(platform("androidx.compose:compose-bom:2024.04.01"))
		implementation("androidx.compose.ui:ui-tooling-preview")
		implementation("androidx.wear.compose:compose-material:1.3.1")
		implementation("androidx.wear.compose:compose-foundation:1.3.1")
		implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
		implementation("androidx.activity:activity-compose:1.9.0")
		debugImplementation("androidx.compose.ui:ui-tooling")

		// WorkManager for background tasks
		implementation("androidx.work:work-runtime-ktx:2.9.0")
}
