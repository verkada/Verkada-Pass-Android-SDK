pluginManagement {
    run {
        settings.extra["_githubUser"] = "x-access-token"

        // Check for macOS (same approach Gradle's internal OperatingSystem class uses)
        val isMac = System.getProperty("os.name").contains("Mac OS", ignoreCase = true)

        // Try macOS keychain first (using providers.exec for configuration cache compatibility)
        val keychainPassword = if (isMac) {
            try {
                providers.exec {
                    commandLine("security", "find-internet-password", "-s", "maven.pkg.github.com", "-a", "x-access-token", "-w")
                    isIgnoreExitValue = true
                }.standardOutput.asText.orNull?.trim()?.takeIf { it.isNotEmpty() }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        // Fall back to gradle.properties or environment variable
        val gradlePropertiesPassword = settings.extra.properties["githubToken"] as? String
        val envPassword = providers.environmentVariable("GITHUB_TOKEN").orNull

        settings.extra["_githubPassword"] = keychainPassword ?: gradlePropertiesPassword ?: envPassword

        // Warn if on macOS and using plaintext gradle.properties
        if (isMac && keychainPassword == null && gradlePropertiesPassword != null) {
            logger.warn("⚠️  WARNING: Using plaintext GitHub token from ~/.gradle/gradle.properties")
            logger.warn("   This is insecure! Use macOS Keychain instead:")
            logger.warn("   ./scripts/setup_github_keychain.sh")
        }
    }

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
        mavenLocal()
        google()
        mavenCentral()
        maven { url = uri("https://www.jitpack.io") }

        exclusiveContent {
            forRepository {
                maven {
                    url = uri("https://maven.pkg.github.com/verkada/Verkada-Pass-Android-SDK")
                    credentials {
                        username = settings.extra["_githubUser"] as? String
                        password = settings.extra["_githubPassword"] as? String
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
