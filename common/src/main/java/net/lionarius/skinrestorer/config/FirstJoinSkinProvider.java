package net.lionarius.skinrestorer.config;

import com.google.gson.annotations.SerializedName;
import net.lionarius.skinrestorer.skin.provider.ElyBySkinProvider;
import net.lionarius.skinrestorer.skin.provider.MojangSkinProvider;

public enum FirstJoinSkinProvider {
    @SerializedName(value = "MOJANG", alternate = {"mojang"})
    MOJANG(MojangSkinProvider.PROVIDER_NAME),
    @SerializedName(value = "ELY.BY", alternate = {"ely.by", "ELY_BY", "ely_by"})
    ELY_BY(ElyBySkinProvider.PROVIDER_NAME);
    
    private final String name;
    
    FirstJoinSkinProvider(String name) {
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }
}
