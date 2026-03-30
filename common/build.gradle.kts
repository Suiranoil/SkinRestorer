plugins {
    id("multiloader-common")
    id("fabric-loom")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")

    mappings(
        loom.layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-${property("parchment_minecraft")}:${property("parchment_version")}@zip")
        },
    )

    compileOnly("org.spongepowered:mixin:0.8.5")

    implementation("org.mineskin:java-client:${property("mineskin_client_version")}")
}

loom {
    val awPath = project.file("src/main/resources/${property("mod_id")}.accesswidener")
    if (awPath.exists()) {
        accessWidenerPath = awPath
    }
}

val commonJava: Configuration by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val commonResources: Configuration by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

artifacts {
    val mainSourceSet = sourceSets.main.get()

    mainSourceSet.java.sourceDirectories.files.forEach {
        add(commonJava.name, it)
    }

    mainSourceSet.resources.sourceDirectories.files.forEach {
        add(commonResources.name, it)
    }
}
