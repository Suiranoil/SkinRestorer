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

            skinMap.putIfAbsent(uuid, skin);
        }

        return skinMap.get(uuid);
    }

    public SkinValue getSkin(UUID uuid) {
        return this.getSkin(uuid, true);
    }

    public void removeSkin(UUID uuid) {
        this.skinMap.remove(uuid);
    }

    public void deleteSkin(UUID uuid) {
        this.removeSkin(uuid);
        this.skinIO.deleteSkin(uuid);
    }

    public void setSkin(UUID uuid, SkinValue skin) {
        if (skin == null) skin = SkinValue.EMPTY;

        var previous = skinMap.get(uuid);
        if (previous != null && skin.originalValue() == null) skin = skin.setOriginalValue(previous.originalValue());

        // write-through so a server crash doesn't lose skin changes;
        // skipped when the value is unchanged (e.g. the original skin refresh on join)
        if (skin.equals(previous)) return;

        skinMap.put(uuid, skin);
        skinIO.saveSkin(uuid, skin);
    }
}
