pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "arcta-example"

includeBuild("..") {
    dependencySubstitution {
        substitute(module("io.gatlio:core")).using(project(":core"))
        substitute(module("io.gatlio:compose")).using(project(":compose"))
    }
}

include(":app")
