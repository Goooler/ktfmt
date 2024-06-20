pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }

    // Include 'plugins build' to define convention plugins.
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenLocal()
    }
}

rootProject.name = "ktfmt-parent"

include(
    ":core",
    ":ktfmt_idea_plugin",
    ":online_formatter",
)
