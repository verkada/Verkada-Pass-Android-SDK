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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://www.jitpack.io") }

        // TODO(revert): Temporary for includeBuild(../Verkada-Pass-Android-SDK). Remove when SDK is consumed as a published artifact.
        exclusiveContent {
            forRepository {
                maven {
                    url = uri("https://maven.pkg.github.com/verkada/Verkada-Android-Library")
                    credentials {
                        val githubUsername: String? by settings
                        val githubToken: String? by settings
                        username = githubUsername ?: System.getenv("GITHUB_USERNAME")
                        password = githubToken ?: System.getenv("GITHUB_TOKEN")
                    }
                }
            }
            filter {
                includeGroupByRegex("com\\.verkada\\..*")
            }
        }
    }
}

rootProject.name = "Verkada-Pass-Android-SDK-Sample"

includeBuild("../Verkada-Pass-Android-SDK")
include(":mobile")
include(":core")
include(":wear")
