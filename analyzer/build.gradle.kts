group = "org.jboss.gm.analyzer"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

gradlePlugin {
    plugins {
        create("alignmentPlugin") {
            id = "org.jboss.gm.analyzer"
            implementationClass = "org.jboss.gm.analyzer.alignment.AlignmentPlugin"
        }
    }
}

dependencies {
    compile(project(":common"))
    // the shadow configuration is used in order to avoid adding gradle and groovy stuff to the shadowed jar
    shadow(localGroovy())
    shadow(gradleApi())

    compile("commons-beanutils:commons-beanutils:1.9.3")
    compile("org.commonjava.maven.ext:pom-manipulation-core:${extra.get("pmeVersion")}")
    testCompile("junit:junit:4.12")
    testCompile("com.github.stefanbirkner:system-rules:1.19.0")
    testCompile("org.jboss.byteman:byteman:${extra.get("bytemanVersion")}")
    testCompile("org.jboss.byteman:byteman-bmunit:${extra.get("bytemanVersion")}")
    testCompile("org.jboss.byteman:byteman-submit:${extra.get("bytemanVersion")}")
    testCompile("org.jboss.byteman:byteman-install:${extra.get("bytemanVersion")}")
    if ( ! JavaVersion.current().isJava9Compatible) {
        testCompile (files ("${System.getProperty("java.home")}/../lib/tools.jar") )
    }
    testCompile("org.assertj:assertj-core:3.12.2")
    testCompile("org.mockito:mockito-core:2.27.0")
    testCompile("com.github.tomakehurst:wiremock-jre8:2.23.2")
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
    //this will be used in the Wiremock tests - the port needs to match what Wiremock is setup to use
    environment("DA_ENDPOINT_URL", "http://localhost:8089/da/rest/v-1")
}

tasks.get("check").dependsOn(functionalTest)

tasks {
    "processResources"(ProcessResources::class) {
         filesMatching("gme.gradle") {
            expand(project.properties)
        }
    }
}
