// Root build file. Plugins are declared `apply false` here so that every
// subproject resolves the same version from gradle/libs.versions.toml.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt)
}

// Detekt is applied to every Kotlin module. `detekt-formatting` wraps the ktlint
// rule set, so a single tool covers both the "detekt" and "ktlint" gates the PRD
// asks for. See /project_memory/architecture_decisions.md ADR-0009.
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    detekt {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        baseline = rootProject.file("config/detekt/baseline.xml").takeIf { it.exists() }
        parallel = true
    }

    dependencies {
        add("detektPlugins", rootProject.libs.detekt.formatting)
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = rootProject.libs.versions.jvmTarget.get()
        reports {
            html.required.set(true)
            xml.required.set(true)
            sarif.required.set(true)
            txt.required.set(false)
            md.required.set(false)
        }
    }
    tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
        jvmTarget = rootProject.libs.versions.jvmTarget.get()
    }
}

// Convenience aggregate: `./gradlew qualityCheck` is what CI and the building agent run
// before every commit.
//
// IMPORTANT — why this enumerates test tasks by module type rather than just calling
// `testDebugUnitTest`: `:domain` is a pure Kotlin/JVM module, so its test task is `test`,
// not `testDebugUnitTest`. Running only `testDebugUnitTest` silently skips the entire
// domain test suite — which is where the workout engine's property tests live, i.e. the
// most important tests in the project. Gradle reports success either way, so the gap is
// invisible. If you add a module, make sure it is covered here.
tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs detekt (incl. ktlint formatting rules) and every module's unit tests."
    dependsOn(subprojects.map { "${it.path}:detekt" })
    dependsOn(
        subprojects
            // `:core` is a container with no build file of its own, so it has no test
            // task at all. Including it fails configuration with "Task with path
            // ':core:test' not found".
            .filter { it.buildFile.exists() }
            .map { project ->
                // Android modules get a per-variant test task; JVM modules get plain `test`.
                val isAndroid = project.path != ":domain"
                if (isAndroid) "${project.path}:testDebugUnitTest" else "${project.path}:test"
            },
    )
}
