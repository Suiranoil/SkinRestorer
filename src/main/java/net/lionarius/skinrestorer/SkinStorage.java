package net.lionarius.skinrestorer;

import com.mojang.authlib.properties.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkinStorage {

    public static final Property DEFAULT_SKIN = new Property("textures", "ewogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJpZCIgOiAiZWZhMTFjN2U1YThlNGIwM2JjMDQ0MWRmNzk1YjE0YjIiLAogICAgICAidHlwZSIgOiAiU0tJTiIsCiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTYzNDM4OTUzYTc4MmRhNzY5NDgwYjBhNDkxMjVhOTJlMjU5MjA3NzAwY2I4ZTNlMWFhYzM4ZTQ3MWUyMDMwOCIsCiAgICAgICJwcm9maWxlSWQiIDogImZkNjBmMzZmNTg2MTRmMTJiM2NkNDdjMmQ4NTUyOTlhIiwKICAgICAgInRleHR1cmVJZCIgOiAiOTYzNDM4OTUzYTc4MmRhNzY5NDgwYjBhNDkxMjVhOTJlMjU5MjA3NzAwY2I4ZTNlMWFhYzM4ZTQ3MWUyMDMwOCIKICAgIH0KICB9LAogICJza2luIiA6IHsKICAgICJpZCIgOiAiZWZhMTFjN2U1YThlNGIwM2JjMDQ0MWRmNzk1YjE0YjIiLAogICAgInR5cGUiIDogIlNLSU4iLAogICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS85NjM0Mzg5NTNhNzgyZGE3Njk0ODBiMGE0OTEyNWE5MmUyNTkyMDc3MDBjYjhlM2UxYWFjMzhlNDcxZTIwMzA4IiwKICAgICJwcm9maWxlSWQiIDogImZkNjBmMzZmNTg2MTRmMTJiM2NkNDdjMmQ4NTUyOTlhIiwKICAgICJ0ZXh0dXJlSWQiIDogIjk2MzQzODk1M2E3ODJkYTc2OTQ4MGIwYTQ5MTI1YTkyZTI1OTIwNzcwMGNiOGUzZTFhYWMzOGU0NzFlMjAzMDgiCiAgfSwKICAiY2FwZSIgOiBudWxsCn0=",
            "T6Czh1iATQTwG/ppZyY9N7cNASVHfGkiicrFykYAve4C7vG36ql0EPf6gMfMIS2eL0FdGLznnWiEC2dUxwNCJwiEyzTo/chlxZMk4TSzkBdBU3KTUZdNZrS/YhTzhi7C4eUVaEtXMRlCVtLQUa8Nb18SFYz243C9tlDsONNk42+xHPN1vRCRGIxfJbcU/mk4/XZzS4zHwPCkB6N4dKX2F6LA+a2P+CUMBluXKF56UiT1j7DjWs8B+6ES0kkmZUGkRaxTtcyN2Rqpx/2wCroohxkyVRAdlkcnwbEHOEKGoYMKdjUWpSm8QrsLkUiyLL3IK/hgd5ET2nI/aE1AloAwr1fotmvf9KF1JIfZljoefYZIaYZ1PpvduwIkAaeeIC4FFcdcBIheHitYyXOBAr/t5E+pTzCJOttDfYggFSyGxOj5yxgXTT4gSwTKp5zkQqiCKdAQQPmgFqxhWkZ2UaE9zq+E5jSOD0OJj3FmBscdZWKoOm+mWZkXbw9z2ZvuqXAKHsi6uVJyGeUzt2hJL8eqOyAmfYsJgfxhGZen5oOlxZra8OxIYlp8TcTwzEIDievgp0dfsGPObGVgtA8D39QiwLXs6e/o0qnzl3+wQJDa/ZqDMISULkBNhPx/TvhYW5MJw3hZIj2gsbf73n+jId1GOUfTVMaFlVf7pvPNqW0PieY=");

    private final Map<UUID, Property> skinMap = new HashMap<>();
    private final SkinIO skinIO;

    public SkinStorage(SkinIO skinIO) {
        this.skinIO = skinIO;
    }

    public Property getSkin(UUID uuid) {
        if (!skinMap.containsKey(uuid)) {
            Property skin = skinIO.loadSkin(uuid);
            setSkin(uuid, skin);
        }

        return skinMap.get(uuid);
    }

    public void removeSkin(UUID uuid) {
        if (skinMap.containsKey(uuid)) {
            skinIO.saveSkin(uuid, skinMap.get(uuid));
        }
    }

    public void setSkin(UUID uuid, Property skin) {
        if (skin == null)
            skin = DEFAULT_SKIN;

        skinMap.put(uuid, skin);
    }
}
