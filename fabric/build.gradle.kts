plugins {
    id("fabric-loom")
    id("multiloader-loader")
    id("multiloader-publish")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")

    mappings(
        loom.layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-${property("parchment_minecraft")}:${property("parchment_version")}@zip")
        },
    )

    modImplementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    modCompileOnly("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")

    include(implementation("org.mineskin:java-client:${property("mineskin_client_version")}")!!)
}

loom {
    val awPath = project(":common").file("src/main/resources/${property("mod_id")}.accesswidener")
    if (awPath.exists()) {
        accessWidenerPath = awPath
    }

    mixin {
        defaultRefmapName = "${property("mod_id")}.refmap.json"
    }

    runs {
        named("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
            runDir = "../run/client"
        }

        named("server") {
            server()
            configName = "Fabric Server"
            ideConfigGenerated(true)
            runDir = "../run/server"
        }
    }
}
