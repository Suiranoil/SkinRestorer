package net.lionarius.skinrestorer.skin;

import net.lionarius.skinrestorer.util.PlayerUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
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

    public boolean setSkin(UUID uuid, SkinValue skin, boolean cache) {
        if (skin == null) skin = SkinValue.EMPTY;

        var previous = cache ? skinMap.get(uuid) : (this.hasSavedSkin(uuid) ? this.getSkin(uuid, false) : null);
        if (previous != null && skin.originalValue() == null) skin = skin.setOriginalValue(previous.originalValue());

        // skinMap only ever holds connected players, so a write for someone who isn't
        // connected must evict instead of caching, otherwise nothing would ever remove it
        if (!cache) skinMap.remove(uuid);

        // write-through so a server crash doesn't lose skin changes;
        // skipped when the value is unchanged (e.g. the original skin refresh on join)
        if (SkinStorage.areSkinValuesEqual(skin, previous)) return false;

        if (cache) skinMap.put(uuid, skin);
        skinIO.saveSkin(uuid, skin);

        return true;
    }

    public boolean setSkin(UUID uuid, SkinValue skin) {
        return this.setSkin(uuid, skin, true);
    }

    private static boolean areSkinValuesEqual(@Nullable SkinValue first, @Nullable SkinValue second) {
        if (first == second) return true;
        if (first == null || second == null) return false;

        return first.provider().equals(second.provider())
                && Objects.equals(first.argument(), second.argument())
                && first.variant() == second.variant()
                && PlayerUtils.areSkinPropertiesEquals(first.value(), second.value())
                && PlayerUtils.areSkinPropertiesEquals(first.originalValue(), second.originalValue());
    }
}
