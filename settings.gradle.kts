pluginManagement {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
        if (System.getenv("AProxDependencyUrl") != null) {
            maven {
                url = uri(System.getenv("AProxDependencyUrl"))
            }
        }
    }
}
rootProject.name = "gradle-manipulator"
include("analyzer")
include("manipulation")
include("common")
