pluginManagement {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
        maven {
            url = uri("https://indy-proxy.newcastle-stage.svc.cluster.local/m2/")
        }
    }
}
rootProject.name = "gradle-manipulator"
include("analyzer")
include("manipulation")
include("common")
