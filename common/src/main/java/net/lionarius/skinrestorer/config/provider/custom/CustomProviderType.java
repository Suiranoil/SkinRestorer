package net.lionarius.skinrestorer.config.provider.custom;

import com.google.gson.annotations.SerializedName;

public enum CustomProviderType {
    @SerializedName(value = "yggdrasil", alternate = {"YGGDRASIL"})
    YGGDRASIL("yggdrasil"),
    @SerializedName(value = "authlib-injector", alternate = {"AUTHLIB_INJECTOR"})
    AUTHLIB_INJECTOR("authlib-injector"),
    @SerializedName(value = "unknown", alternate = {"UNKNOWN"})
    UNKNOWN("unknown");

    private final String name;

    CustomProviderType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
