plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.dibitara.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dibitara.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "2.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    // Rapports de couverture Kover
    kover {
        reports {
            filters {
                includes {
                    // Mesurer uniquement la couche domain (UseCases + modèles métier)
                    classes("com.dibitara.app.domain.*")
                }
                excludes {
                    // Exclure les classes générées automatiquement
                    classes("*_Factory*", "*_HiltModules*", "*_Impl*", "Hilt_*")
                }
            }
            verify {
                // Le CI échoue si la couverture sur domain/ tombe sous 80%
                rule { minBound(80) }
            }
        }
    }

    // Emplacement des schémas Room exportés — nécessaire pour les migrations vérifiables
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    // Répertoires de tests
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all { it.useJUnitPlatform() }
    }
}

dependencies {
    // Core Android
    implementation(libs.appcompat)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt — injection de dépendances
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room — base de données locale chiffrée
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore — préférences persistantes
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.coroutines.android)

    // Sécurité
    implementation(libs.biometric)
    implementation(libs.security.crypto)

    // Graphiques
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.zxing.core)

    // Tests unitaires
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.room.testing)

    // Tests instrumentés (UI)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.espresso.core)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
