package net.lionarius.skinrestorer.config.provider;

public interface BuiltInProviderConfig {

    boolean enabled();

    String name();

    CacheConfig cache();
}
