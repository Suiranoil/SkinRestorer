plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.3.0")
    implementation("me.modmuss50:mod-publish-plugin:1.1.+")
}
