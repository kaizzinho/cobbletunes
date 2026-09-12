plugins {
    kotlin("jvm") version "2.2.20"
    id("fabric-loom") version "1.10.1"
    `maven-publish`
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

repositories {
    mavenCentral()
    maven("https://artefacts.cobblemon.com/releases/") { name = "Cobblemon" }
    maven("https://api.modrinth.com/maven") { name = "Modrinth" }
    maven("https://maven.terraformersmc.com/") { name = "Terraformers" }
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("cobbletunes") {
            sourceSet("main")
            sourceSet("client")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("fabric_kotlin_version")}")

    // mod menu is optional
    modImplementation("com.terraformersmc:modmenu:${project.property("modmenu_version")}")

    modImplementation("com.cobblemon:fabric:${project.property("cobblemon_version")}")

    // rct stays optional; local runtime loads it for trainer tests
    modCompileOnly(files("libs/rctapi-fabric-1.21.1-0.16.0-beta.jar"))
    modCompileOnly(files("libs/rctmod-fabric-1.21.1-0.19.0-beta.jar"))
    modLocalRuntime(files("libs/rctapi-fabric-1.21.1-0.16.0-beta.jar"))
    modLocalRuntime(files("libs/rctmod-fabric-1.21.1-0.19.0-beta.jar"))
    modLocalRuntime(files("libs/architectury-13.0.11-fabric.jar"))
    modLocalRuntime(files("libs/ForgeConfigAPIPort-v21.1.6-1.21.1-Fabric.jar"))
    implementation("com.electronwill.night-config:core:3.8.0")
    implementation("com.electronwill.night-config:toml:3.8.0")
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.property("archives_base_name")}" }
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(mutableMapOf("version" to project.version))
    }
    if (project.hasProperty("excludeAudio")) {
        exclude("assets/cobbletunes/sounds/**/*.ogg")
    }
}