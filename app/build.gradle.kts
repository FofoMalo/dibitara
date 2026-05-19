import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kover)
    alias(libs.plugins.gpp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Lecture des secrets de signature depuis keystore.properties (hors dépôt git).
// Le fichier est absent sur CI (gitignored) — la signing config release est donc
// créée uniquement quand il est présent, pour ne pas bloquer lint/tests/debug.
val keystoreFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystoreFile.exists()) load(keystoreFile.inputStream())
}

android {
    namespace = "com.dibitara.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dibitara.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "3.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Activé uniquement si google-services.json est présent (Firebase configuré)
        val googleServicesFile = rootProject.file("app/google-services.json")
        buildConfigField("boolean", "CRASHLYTICS_ENABLED", googleServicesFile.exists().toString())
    }

    buildFeatures {
        buildConfig = true
        compose     = true
    }

    if (keystoreFile.exists()) {
        signingConfigs {
            create("release") {
                storeFile     = file(keystoreProps.getProperty("storeFile")!!)
                storePassword = keystoreProps.getProperty("storePassword")!!
                keyAlias      = keystoreProps.getProperty("keyAlias")!!
                keyPassword   = keystoreProps.getProperty("keyPassword")!!
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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

// Couverture de code — filtre domain/ et impose un seuil 80% en CI.
// Le bloc est au niveau projet (pas dans android {}) — c'est requis par Kover 0.8.x.
kover {
    reports {
        filters {
            includes {
                // Mesurer uniquement la couche métier (UseCases + modèles)
                classes("com.dibitara.app.domain.*")
            }
            excludes {
                // Ignorer les classes générées par Hilt, KSP, Room
                classes("*_Factory*", "*_HiltModules*", "*_Impl*", "Hilt_*")
            }
        }
        total {
            // Rapport HTML généré manuellement (./gradlew koverHtmlReport) — pas à chaque build
            html { onCheck = false }
        }
        verify {
            rule {
                // Le CI échoue si la couverture tombe sous 80%
                bound { minValue = 80 }
            }
        }
    }
}

// Publication automatique vers le Play Store via gradle-play-publisher.
// Prérequis : créer play-service-account.json (voir README) et ne pas le committer.
val playCredentialsFile = rootProject.file("play-service-account.json")
if (playCredentialsFile.exists()) {
    configure<com.github.triplet.gradle.play.PlayPublisherExtension> {
        serviceAccountCredentials.set(playCredentialsFile)
        // "internal" = piste de test interne ; changer en "alpha", "beta" ou "production" si besoin
        track.set("internal")
        // Envoyer le AAB plutôt que l'APK
        defaultToAppBundles.set(true)
        // Ne pas publier automatiquement — laisser la main à la Play Console
        releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.DRAFT)
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

    // Firebase — suivi des crashs en production (Crashlytics)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    // Réseau — taux de change (API Frankfurter)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

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
