package net.lionarius.skinrestorer.skin;

import com.google.gson.annotations.SerializedName;

public enum SkinVariant {
    @SerializedName(value = "classic", alternate = {"CLASSIC"})
    CLASSIC("classic"),
    @SerializedName(value = "slim", alternate = {"SLIM"})
    SLIM("slim");
    
    private final String name;
    
    SkinVariant(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
