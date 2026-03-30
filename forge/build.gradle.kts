plugins {
    id("idea")
    id("eclipse")

    id("net.minecraftforge.gradle")
    id("net.minecraftforge.jarjar") version "0.2.3"

    id("multiloader-loader")
    id("multiloader-publish")
}

jarJar.register {
    archiveClassifier = null
}

tasks.named<Jar>("jar") {
    archiveClassifier = "slim"

    manifest {
        attributes(
            mapOf(
                "MixinConfigs" to "${project.property("mod_id")}.mixins.json",
            ),
        )
    }
}

minecraft {
    mappings("official", project.property("minecraft_version").toString())

    val atFile = project(":common").file("src/main/resources/META-INF/accesstransformer.cfg")

    if (atFile.exists() && atFile.length() > 0) {
        useDefaultAccessTransformer()
    }

    runs {
        configureEach {
            args("--mixin.config=${project.property("mod_id")}.mixins.json")
        }

        create("client") {
            workingDir = rootProject.file("run/client")
        }

        create("server") {
            args("--nogui")
            workingDir = rootProject.file("run/server")
        }
    }
}

repositories {
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
}

dependencies {
    implementation(
        minecraft.dependency(
            "net.minecraftforge:forge:${project.property("forge_minecraft_version")}-${project.property("forge_version")}",
        ),
    )

    annotationProcessor("net.minecraftforge:eventbus-validator:7.0.1")

    // Define the dependency as a variable, then add it to both configurations
    val mineskinDep = "org.mineskin:java-client:${project.property("mineskin_client_version")}"
    implementation(mineskinDep)
    "jarJar"(mineskinDep)
}

sourceSets.forEach {
    val dir = layout.buildDirectory.dir("sourcesSets/${it.name}")
    it.output.setResourcesDir(dir)
    it.java.destinationDirectory = dir
}

tasks.named<ProcessResources>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
