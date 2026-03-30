plugins {
    id("java-library")
    id("com.diffplug.spotless")
}

base {
    archivesName.set("${project.property("mod_id")}")
    version = "${project.property("mod_version")}+${project.property("minecraft_version")}-${project.name}"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(project.property("java_version").toString().toInt()))
    withSourcesJar()
}

spotless {
    java {
        palantirJavaFormat()
        importOrder("", "javax", "java", "\\#")
        trimTrailingWhitespace()
        endWithNewline()
        toggleOffOn()
    }
}

repositories {
    mavenCentral()

    maven {
        name = "ParchmentMC"
        url = uri("https://maven.parchmentmc.org")
    }
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net")
    }
    maven {
        name = "Forge"
        url = uri("https://maven.minecraftforge.net")
    }
    maven {
        name = "NeoForge"
        url = uri("https://maven.neoforged.net/releases")
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "Sponge"
                url = uri("https://repo.spongepowered.org/repository/maven-public")
            }
        }
        filter {
            includeGroupAndSubgroups("org.spongepowered")
        }
    }
}

// Capabilities
listOf("apiElements", "runtimeElements", "sourcesElements").forEach { variant ->
    configurations.named(variant) {
        outgoing {
            capability("${project.group}:${base.archivesName.get()}:${project.version}")
            capability("${project.group}:${project.property("mod_id")}-${project.name}:${project.version}")
            capability("${project.group}:${project.property("mod_id")}:${project.version}")
        }
    }
}

tasks.named<Jar>("sourcesJar") {
    from(rootProject.file("LICENSE"))
}

tasks.named<Jar>("jar") {
    from(rootProject.file("LICENSE"))

    manifest {
        attributes(
            mapOf(
                "Specification-Title" to project.property("mod_name"),
                "Specification-Vendor" to project.property("mod_author"),
                "Specification-Version" to archiveVersion.get(),
                "Implementation-Title" to project.name,
                "Implementation-Version" to archiveVersion.get(),
                "Implementation-Vendor" to project.property("mod_author"),
                "Built-On-Minecraft" to project.property("minecraft_version"),
            ),
        )
    }
}

tasks.processResources {
    val expandProps =
        mapOf(
            "version" to project.version,
            "group" to project.group,
            "minecraft_version" to project.property("minecraft_version"),
            "minecraft_version_range" to project.property("minecraft_version_range"),
            "mod_name" to project.property("mod_name"),
            "mod_author" to project.property("mod_author"),
            "mod_id" to project.property("mod_id"),
            "mod_homepage" to project.property("mod_homepage"),
            "mod_sources" to project.property("mod_sources"),
            "mod_issues" to project.property("mod_issues"),
            "license" to project.property("license"),
            "description" to project.description,
            "credits" to project.property("credits"),
            "java_version" to project.property("java_version"),
            // Loader-specific
            "fabric_loader_version" to (project.findProperty("fabric_loader_version") ?: ""),
            "forge_version" to (project.findProperty("forge_version") ?: ""),
            "forge_loader_version_range" to (project.findProperty("forge_loader_version_range") ?: ""),
            "neoforge_version" to (project.findProperty("neoforge_version") ?: ""),
            "neoforge_loader_version_range" to (project.findProperty("neoforge_loader_version_range") ?: ""),
        )

    inputs.properties(expandProps)

    filesMatching(
        listOf(
            "pack.mcmeta",
            "fabric.mod.json",
            "META-INF/mods.toml",
            "META-INF/neoforge.mods.toml",
            "*.mixins.json",
        ),
    ) {
        expand(expandProps)
    }
}
