pluginManagement {
    repositories {
        google()        // Required for Android plugins
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()        // Required for Android libraries (OkHttp, etc)
        mavenCentral()
    }
}

rootProject.name = "HydraClient"
include(":app")