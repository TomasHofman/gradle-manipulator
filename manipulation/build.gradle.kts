group = "org.jboss.gm.manipulation"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

pluginBundle {
    description = "Plugin that reads the alignment data from \${project.rootDir}/manipulation.json and configures build and publishing to use those versions"
    website = "https://project-ncl.github.io/gradle-manipulator/"
    vcsUrl = "https://github.com/project-ncl/gradle-manipulator/tree/master/manipulation/tree/master/analyzer"
    tags = listOf("versions", "manipulation")
}

gradlePlugin {
    plugins {
        create("manipulationPlugin") {
            id = "org.jboss.gm.manipulation"
            implementationClass = "org.jboss.gm.manipulation.ManipulationPlugin"
            displayName = "gme-manipulation"
        }
    }
}

dependencies {
    compile(project(":common"))
    // the shadow configuration is used in order to avoid adding gradle and groovy stuff to the shadowed jar
    shadow(localGroovy())
    shadow(gradleApi())
    testCompile("junit:junit:4.12")
    testCompile("org.assertj:assertj-core:3.12.2")
    testCompile("com.github.stefanbirkner:system-rules:1.19.0")
}

// separate source set and task for functional tests

sourceSets.create("functionalTest") {
    java.srcDir("src/functTest/java")
    resources.srcDir("src/functTest/resources")
    compileClasspath += sourceSets["main"].output + configurations.testRuntime
    runtimeClasspath += output + compileClasspath
}

val functionalTest = task<Test>("functionalTest") {
    description = "Runs functional tests"
    group = "verification"
    testClassesDirs = sourceSets["functionalTest"].output.classesDirs
    classpath = sourceSets["functionalTest"].runtimeClasspath
    mustRunAfter(tasks["test"])
}

tasks.get("check").dependsOn(functionalTest)
