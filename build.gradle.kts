import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.text.SimpleDateFormat
import java.util.*

plugins {
    java
    id("com.diffplug.gradle.spotless") version "3.21.0"
    id("com.github.johnrengelman.shadow") version "4.0.4"
    id("org.ajoberstar.grgit") version "3.1.1"
    id("com.gradle.plugin-publish") version "0.10.1"
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }
    }
}

subprojects {

    extra["bytemanVersion"] = "4.0.6"
    extra["pmeVersion"] = "3.6.1"

    apply(plugin = "com.diffplug.gradle.spotless")

    spotless {
        java {
            importOrderFile("$rootDir/ide-config/eclipse.importorder")
            eclipse().configFile("$rootDir/ide-config/eclipse-format.xml")
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        dependsOn("spotlessApply")
    }

    if (project.name == "common") {
        apply(plugin = "java-library")
    } else {
        apply(plugin = "java")
        apply(plugin = "maven-publish")
        apply(plugin = "java-gradle-plugin")
        apply(plugin = "com.github.johnrengelman.shadow")
        apply(plugin = "com.gradle.plugin-publish")

        /**
         * The configuration below has been created by reading the documentation at:
         * https://imperceptiblethoughts.com/shadow/plugins/
         *
         * Another great source of information is the configuration of the shadow plugin itself:
         * https://github.com/johnrengelman/shadow/blob/master/build.gradle
         */

        // We need to do this otherwise the shadowJar tasks takes forever since
        // it tries to shadow the entire gradle api
        // Moreover, the gradleApi and groovy dependencies in the plugins themselves
        // have been explicitly declared with the shadow configuration
        configurations.get("compile").dependencies.remove(dependencies.gradleApi())

        // make build task depend on shadowJar
        val build: DefaultTask by tasks
        val shadowJar = tasks["shadowJar"] as ShadowJar
        build.dependsOn(shadowJar)

        tasks.withType<ShadowJar>() {
            // ensure that a single jar is built which is the shadowed one
            classifier = ""
            dependencies {
                exclude(dependency("org.slf4j:slf4j-api:1.7.25"))
            }
            // no need to analyzer.init.gradle in the jar since it will never be used from inside the plugin itself
            exclude("analyzer.init.gradle")
        }

        val sourcesJar by tasks.registering(Jar::class) {
            classifier = "sources"
            from(sourceSets.get("main").allSource)
        }

        val javadocJar by tasks.registering(Jar::class) {
            classifier = "javadoc"
            from(tasks["javadoc"])
        }

        // configure publishing of the shadowJar
        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("shadow") {
                    project.shadow.component(this)
                    artifact(sourcesJar.get())
                    artifact(javadocJar.get())
                    // we publish the init gradle file to make it easy for tools that use
                    // the plugin to set it up without having to create their own init gradle file
                    if (project.name == "analyzer") {
                        artifact("${sourceSets.get("main").output.resourcesDir}/analyzer.init.gradle", {
                            extension = "init.gradle"
                        })
                    }
                }
            }
            repositories {
                var deployUrl = System.getProperty("AProxDeployUrl")
                if (deployUrl == null) {
                    deployUrl = System.getenv("AProxDeployUrl")
                }
                var accessToken = System.getProperty("accessToken")
                if (accessToken == null) {
                    accessToken = System.getenv("accessToken")
                }
                if (deployUrl != null) {
                    maven {
                        url = uri(deployUrl)
                        if (accessToken != null) {
                            credentials(HttpHeaderCredentials::class) {
                                name = "Authorization"
                                value = "Bearer " + accessToken
                            }
                            authentication {
                                create("header", HttpHeaderAuthentication::class)
                            }
                        }
                    }
                }
            }

        }
    }

    // Exclude logback from dependency tree/
    configurations {
        "compile" {
            exclude(group="ch.qos.logback", module="logback-classic")
        }
        "compile" {
            exclude(group="ch.qos.logback", module="logback-core")
        }
    }

    // Retrieve commit id for jar manifest
    val git = org.ajoberstar.grgit.Grgit.open()
    val gitCommitId = git.head().id

    tasks {
        "jar"(Jar::class) {
            this.manifest {
                attributes["Built-By"]=System.getProperty("user.name")
                attributes["Build-Timestamp"]= SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(Date())
                attributes["Scm-Revision"]=gitCommitId
                attributes["Created-By"]="Gradle ${gradle.gradleVersion}"
                attributes["Build-Jdk"]=System.getProperty("java.version") + " ; " + System.getProperty("java.vendor") + " ; " + System.getProperty("java.vm.version")
                attributes["Build-OS"]=System.getProperty("os.name") + " ; " + System.getProperty("os.arch") + " ; " + System.getProperty("os.version")
                attributes["Implementation-Version"]="${project.version}"
            }
        }
    }
}
