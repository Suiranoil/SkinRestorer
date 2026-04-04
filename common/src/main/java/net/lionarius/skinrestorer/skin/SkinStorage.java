package net.lionarius.skinrestorer.skin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkinStorage {
    private final Map<UUID, SkinValue> skinMap = new ConcurrentHashMap<>();
    private final SkinIO skinIO;

    public SkinStorage(SkinIO skinIO) {
        this.skinIO = skinIO;
    }

    public boolean hasSavedSkin(UUID uuid) {
        return this.skinMap.containsKey(uuid) || this.skinIO.skinExists(uuid);
    }

    public SkinValue getSkin(UUID uuid, boolean cache) {
        if (!skinMap.containsKey(uuid)) {
            var skin = skinIO.loadSkin(uuid);
            if (!cache) return skin;

            setSkin(uuid, skin);
        }

        return skinMap.get(uuid);
    }

    public SkinValue getSkin(UUID uuid) {
        return this.getSkin(uuid, true);
    }

    public void removeSkin(UUID uuid, boolean save) {
        var skin = skinMap.remove(uuid);
        if (skin != null && save) skinIO.saveSkin(uuid, skin);
    }

    public void removeSkin(UUID uuid) {
        this.removeSkin(uuid, true);
    }

    public void deleteSkin(UUID uuid) {
        this.removeSkin(uuid, false);
        this.skinIO.deleteSkin(uuid);
    }

    public void setSkin(UUID uuid, SkinValue skin) {
        if (skin == null) skin = SkinValue.EMPTY;

        if (skinMap.containsKey(uuid) && skin.originalValue() == null)
            skin = skin.setOriginalValue(skinMap.get(uuid).originalValue());

        skinMap.put(uuid, skin);
    }
}
