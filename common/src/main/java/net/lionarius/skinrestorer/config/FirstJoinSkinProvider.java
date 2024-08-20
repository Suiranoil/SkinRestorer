package net.lionarius.skinrestorer.config;

import com.google.gson.annotations.SerializedName;
import net.lionarius.skinrestorer.skin.provider.ElyBySkinProvider;
import net.lionarius.skinrestorer.skin.provider.MojangSkinProvider;

public enum FirstJoinSkinProvider {
    MOJANG(MojangSkinProvider.PROVIDER_NAME),
    @SerializedName("ELY.BY")
    ELY_BY(ElyBySkinProvider.PROVIDER_NAME);
    
    private final String name;
    
    FirstJoinSkinProvider(String name) {
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }
}
