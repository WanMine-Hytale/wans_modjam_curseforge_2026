import dev.scaffoldit.hytale.wire.HytaleManifest
import java.net.URI
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

fun property(name: String, default: String = "") = providers.gradleProperty(name).orElse(default).get()

rootProject.name = property("projectName")

plugins {
    id("dev.scaffoldit") version "0.2.+"
}


val hytaleVersion: String? by lazy {
    runCatching {
        val xml = URI("https://maven.hytale.com/release/com/hypixel/hytale/Server/maven-metadata.xml")
            .toURL()
            .readText()
        Regex("<latest>(.*?)</latest>")
            .find(xml)
            ?.groupValues
            ?.get(1)
    }.getOrNull()
}
val serverVersion: String =
    hytaleVersion.takeUnless { it.isNullOrEmpty() }
        ?: property("hytale.server_version", "*")

hytale {
    projectDir = ""
    devserverDir = "/devserver"
    manifest {
        Name = property("projectName", "Wan's HytaleTemplate")
        Group = property("hytale.group", "WanMine")
        Version = property("projectVersion", "1.0.1")
        Description = property("projectDescription", "Hytale Template Mod")

        Authors =
            providers.gradleProperty("hytale.authors")
                .orNull
                ?.split(",")
                ?.map { HytaleManifest.Author(it.trim()) }
                ?: emptyList()
        Website = property("hytale.website")
        ServerVersion = serverVersion
        Main = property("hytale.main")
        IncludesAssetPack =
            providers.gradleProperty("hytale.includeAssetPack")
                .orNull
                ?.toBoolean()
                ?: false
        Dependencies = mapOf(
            "Hytale:EntityModule" to "*",
            "Hytale:NPC" to "*",
            "Hytale:Mounts" to "*",
        )
    }

    devserver {
        Enabled = true
        AllowOp = true
        DisableSentry = true
        DevelopmentMode = false
    }
}

