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
        substitute(module("io.steadpay:core")).using(project(":core"))
        substitute(module("io.steadpay:compose")).using(project(":compose"))
    }
}

include(":app")
