pluginManagement {
    repositories { maven("https://dl.google.com/android/maven2/"); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { maven("https://dl.google.com/android/maven2/"); mavenCentral() }
}
rootProject.name = "Auralis"
include(":app")
