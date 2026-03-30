plugins {
    id("me.modmuss50.mod-publish-plugin")
}

publishMods {
    val archivesName =
        project.extensions
            .getByType<BasePluginExtension>()
            .archivesName
            .get()

    file = project.layout.buildDirectory.file("libs/$archivesName-${project.version}.jar")

    modLoaders.add(project.name)

    val additional = project.findProperty("additional_modloaders") as String?
    if (!additional.isNullOrEmpty()) {
        additional.split(",").forEach {
            modLoaders.add(it)
        }
    }

    type = STABLE
    version = project.version.toString()
    displayName = "[${project.name.replaceFirstChar { it.uppercase() }}] ${project.property("mod_name")} ${project.property("mod_version")}"
    changelog = rootProject.file("CHANGELOG_LATEST.md").readText()

    curseforge {
        projectId = project.property("curseforge_id").toString()

        dryRun = providers.environmentVariable("CURSEFORGE_API_KEY").orNull == null
        accessToken = providers.environmentVariable("CURSEFORGE_API_KEY")

        minecraftVersions.addAll(project.property("minecraft_version_list").toString().split(","))
        serverRequired = true

        val optionalDeps = project.findProperty("optional_dependencies") as String?
        if (!optionalDeps.isNullOrEmpty()) {
            optional(*optionalDeps.split(",").toTypedArray())
        }
    }
    modrinth {
        projectId = project.property("modrinth_id").toString()

        dryRun = providers.environmentVariable("MODRINTH_TOKEN").orNull == null
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")

        minecraftVersions.addAll(project.property("minecraft_version_list").toString().split(","))

        val optionalDeps = project.findProperty("optional_dependencies") as String?
        if (!optionalDeps.isNullOrEmpty()) {
            optional(*optionalDeps.split(",").toTypedArray())
        }
    }
}
