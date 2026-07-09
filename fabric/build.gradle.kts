plugins {
    id("net.fabricmc.fabric-loom")
    id("multiloader-loader")
    id("multiloader-publish")
}

repositories {
    maven {
        name = "TerraformersMC"
        url = uri("https://maven.terraformersmc.com/")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")

    implementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    compileOnly("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    compileOnly("com.terraformersmc:modmenu:${property("modmenu_version")}")

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
