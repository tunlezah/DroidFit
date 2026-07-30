// VisceralFit — Gradle settings.
// Module topology is fixed by /framework/05_architecture.md. Do not add modules
// without first recording an entry in /project_memory/architecture_decisions.md.

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
    // Fail the build if any module declares its own repositories. Keeps the
    // dependency graph auditable, which the offline-first guarantee depends on.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "VisceralFit"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

// Shared, feature-agnostic building blocks.
include(":core:common")
include(":core:designsystem")
include(":core:database")
include(":core:datastore")
include(":core:speech")
include(":core:testing")

// Business rules (pure Kotlin, no Android dependencies) and their implementations.
include(":domain")
include(":data")

// User-facing features. One Gradle module per top-level destination.
include(":feature-workout")
include(":feature-history")
include(":feature-progress")
include(":feature-settings")
