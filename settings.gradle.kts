pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven { url = uri("https://www.jitpack.io") }
        maven {
            url = uri("https://maven.pkg.github.com/verkada/Verkada-Pass-Android-SDK")
            credentials {
                username = "x-access-token"
                password = "<GITHUB_TOKEN>"
            }
        }
    }
}

rootProject.name = "Verkada-Pass-Android-SDK"

include(":mobile")
include(":core")
include(":wear")
