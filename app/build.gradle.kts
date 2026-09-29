import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Clé de signature fixe : variables d'environnement (GitHub Actions) ou signing/keystore.properties (PC).
val signingProps = Properties().apply {
    val f = rootProject.file("signing/keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(env: String): String? = System.getenv(env) ?: signingProps.getProperty(env)
val keystoreFile = signingValue("KEYSTORE_FILE")?.let { file(it) }
    ?: rootProject.file("signing/portailkeo.jks").takeIf { it.exists() }

android {
    namespace = "fr.julesdupont.portail"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.julesdupont.portail"
        minSdk = 26
        targetSdk = 34
        // Numéro de build GitHub = numéro de version, pour que chaque APK remplace le précédent.
        val build = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = build
        versionName = "1.$build"
        buildConfigField("boolean", "TEST_BUILD", "false")
    }

    signingConfigs {
        if (keystoreFile != null && signingValue("KEYSTORE_PASSWORD") != null) {
            create("fixed") {
                storeFile = keystoreFile
                storePassword = signingValue("KEYSTORE_PASSWORD")
                keyAlias = signingValue("KEY_ALIAS")
                keyPassword = signingValue("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Clé fixe si disponible, sinon clé debug (l'APK s'installe quand même).
            signingConfig = signingConfigs.findByName("fixed") ?: signingConfigs.getByName("debug")
        }
        // Version de test : s'installe à côté de la vraie app (autre identifiant),
        // icône orange, outils de simulation, pas de mise à jour automatique.
        create("beta") {
            initWith(getByName("release"))
            applicationIdSuffix = ".test"
            versionNameSuffix = "-test"
            buildConfigField("boolean", "TEST_BUILD", "true")
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    lint { abortOnError = false }
    testOptions {
        // Robolectric : exécute le code Android (SharedPreferences, services…) sur la JVM.
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
}
