import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow") version "9.3.1"
    java
}

group = project.findProperty("projectGroup").toString()
version = project.findProperty("projectVersion").toString()
description = project.findProperty("projectDescription")?.toString()

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}


val shade: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
configurations.implementation.get().extendsFrom(shade)

tasks.withType<Javadoc> {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

tasks.jar {
    archiveBaseName.set(project.name)
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
}

tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shade)

    archiveBaseName.set(rootProject.name)
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    relocate("com.google.gson", "${group}.libs.gson")
    minimize()
}


var testServerDir = file("$projectDir/devserver")
if(!testServerDir.exists()) {
    testServerDir.mkdirs()
}
var testServerModsFolder = "${testServerDir.absolutePath}/mods"

tasks.register<Copy>("buildAndCopyToMods") {
    group = "build"
    description = "Build Plugin and copy on test/mods folder"
    dependsOn(tasks.build)
    from(tasks.jar.map { it.archiveFile })
    into(testServerModsFolder)
    doFirst {
        println("Copying Jar to $testServerModsFolder")
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}