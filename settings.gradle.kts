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
        // AMap SDK mirror (maven.amap.com is unreachable from this network)
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "HajiRacing"
include(":app")
