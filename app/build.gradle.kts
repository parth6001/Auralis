import com.android.build.api.artifact.SingleArtifact

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.auralis.player"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.auralis.player"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    lint { abortOnError = true; checkReleaseBuilds = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
    sourceSets.getByName("androidTest").assets.directories.add("schemas")
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.preview)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.android.test.runner)
    androidTestImplementation(libs.android.test.junit)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
}

// Inspect the merged manifest, including transitive library declarations, for every variant.
androidComponents.onVariants { variant ->
    val manifest = variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)
    val audit = tasks.register("verify${variant.name.replaceFirstChar(Char::uppercase)}Offline") {
        inputs.file(manifest)
        doLast {
            val xml = manifest.get().asFile.readText()
            check(!xml.contains("android.permission.INTERNET")) { "Offline build gained Internet permission" }
            check(!xml.contains("android.permission.ACCESS_NETWORK_STATE")) { "Offline build gained unnecessary network-state permission" }
            val banned = listOf("firebase", "crashlytics", "appsflyer", "adjust-android", "facebook", "amplitude", "mixpanel", "sentry", "okhttp", "retrofit", "volley")
            val modules = configurations.getByName("${variant.name}RuntimeClasspath")
                .resolvedConfiguration.resolvedArtifacts.map { it.moduleVersion.id.toString() }
            check(modules.none { module -> banned.any { module.contains(it, ignoreCase = true) } }) {
                "Prohibited network or tracking dependency"
            }
        }
    }
    tasks.matching { it.name == "assemble${variant.name.replaceFirstChar(Char::uppercase)}" || it.name == "bundle${variant.name.replaceFirstChar(Char::uppercase)}" }.configureEach { dependsOn(audit) }
}

tasks.register("dependencyInventory") {
    doLast {
        val coordinates = configurations.getByName("releaseRuntimeClasspath").resolvedConfiguration.resolvedArtifacts
            .map { it.moduleVersion.id }.distinctBy { it.toString() }.sortedBy { it.toString() }
        val directory = rootProject.file("artifacts").apply { mkdirs() }
        directory.resolve("dependencies.txt").writeText(coordinates.joinToString("\n") { it.toString() } + "\n")
        val components = coordinates.map { mapOf("type" to "library", "group" to it.group, "name" to it.name, "version" to it.version, "purl" to "pkg:maven/${it.group}/${it.name}@${it.version}") }
        directory.resolve("dependencies.cdx.json").writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(mapOf("bomFormat" to "CycloneDX", "specVersion" to "1.5", "version" to 1, "components" to components))))
    }
}
