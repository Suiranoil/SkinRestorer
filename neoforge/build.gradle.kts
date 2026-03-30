plugins {
    id("multiloader-loader")
    id("idea")
    id("eclipse")

    id("net.neoforged.moddev")

    id("multiloader-publish")
}

neoForge {
    version = property("neoforge_version").toString()

    // Automatically enable neoforge AccessTransformers if the file exists
    val atPath = project(":common").file("src/main/resources/META-INF/accesstransformer.cfg")
    if (atPath.exists()) {
        accessTransformers.from(atPath.absolutePath)
        validateAccessTransformers = true
    }

    runs {
        configureEach {
            ideName = "NeoForge ${name.replaceFirstChar { it.uppercase() }} (${project.path})"
        }

        create("client") {
            client()
            gameDirectory = rootProject.file("run/client")
        }

        create("server") {
            server()
            programArgument("--nogui")
            gameDirectory = rootProject.file("run/server")
        }
    }

    mods {
        create(property("mod_id").toString()) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    implementation("net.neoforged:neoforge:${property("neoforge_version")}")

    val mineskinDep = "org.mineskin:java-client:${project.property("mineskin_client_version")}"
    implementation(mineskinDep)
    "jarJar"(mineskinDep)
}
