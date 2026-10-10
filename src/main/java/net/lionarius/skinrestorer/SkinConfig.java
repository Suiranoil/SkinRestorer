package net.lionarius.skinrestorer;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public record SkinConfig(int cooldownSeconds, int requestTimeoutSeconds, int fetchThreads, int queueCapacity,
    int cacheSeconds) {

    public static SkinConfig defaults() {
        return new SkinConfig(30, 10, 4, 64, 300);
    }

    public static SkinConfig load(File file) {
        Configuration config = new Configuration(file);
        config.load();
        SkinConfig result = new SkinConfig(
            config.getInt("cooldownSeconds", "commands", 30, 0, 3600, "Player command cooldown; operators bypass it."),
            config.getInt("requestTimeoutSeconds", "network", 10, 1, 60, "Mojang request timeout."),
            config.getInt("fetchThreads", "network", 4, 1, 8, "Concurrent Mojang requests."),
            config.getInt("queueCapacity", "network", 64, 20, 256, "Maximum queued skin requests."),
            config.getInt("cacheSeconds", "network", 300, 30, 86400, "Mojang response cache lifetime."));
        if (config.hasChanged()) config.save();
        return result;
    }
}
