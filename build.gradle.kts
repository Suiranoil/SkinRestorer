plugins {
    // see https://fabricmc.net/develop/ for new versions
    id("fabric-loom") version "1.15-SNAPSHOT" apply false

    // see https://projects.neoforged.net/neoforged/moddevgradle for new versions
    id("net.neoforged.moddev") version "2.0.+" apply false

    // see https://files.minecraftforge.net/net/minecraftforge/gradle/ForgeGradle/ for new versions
    id("net.minecraftforge.gradle") version "[7,8)" apply false

    id("org.spongepowered.mixin") version "0.7-SNAPSHOT" apply false
    id("com.diffplug.spotless")
}

repositories {
    mavenCentral()
}

spotless {
    kotlinGradle {
        // Target ALL .gradle.kts files in the project recursively from the root
        target("**/*.gradle.kts")

        // Exclude generated folders and buildSrc (which handles itself)
        targetExclude(
            "**/build/**",
            "**/.gradle/**",
        )

        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

allprojects {
    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    name = "lionarius-repository"
                    url = uri("https://reposilite.lionarius.ru/releases")
                }
            }
            filter {
                includeGroupAndSubgroups("org.mineskin")
            }
        }
    }
}
