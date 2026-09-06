package net.lionarius.skinrestorer.translation;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.util.FileUtils;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Map;
import java.util.Objects;

public final class Translation {
    public static final String COMMAND_SKIN_AFFECTED_PLAYERS_KEY = "skinrestorer.command.skin.affected_players";
    public static final String COMMAND_SKIN_RESTORED_PLAYERS_KEY = "skinrestorer.command.skin.restored_players";
    public static final String COMMAND_SKIN_SAVED_FOR_OFFLINE_KEY = "skinrestorer.command.skin.saved_for_offline";
    public static final String COMMAND_SKIN_CLEARED_FOR_OFFLINE_KEY = "skinrestorer.command.skin.cleared_for_offline";
    public static final String COMMAND_SKIN_NO_CHANGES_KEY = "skinrestorer.command.skin.no_changes";
    public static final String COMMAND_SKIN_FAILED_KEY = "skinrestorer.command.skin.failed";
    public static final String COMMAND_SKIN_OK_KEY = "skinrestorer.command.skin.ok";
    public static final String COMMAND_SKIN_LOADING_KEY = "skinrestorer.command.skin.loading";
    public static final String COMMAND_SKIN_CONFIG_RELOADED_KEY = "skinrestorer.command.skin.config_reloaded";

    private static Map<String, String> translations;
    private static final Map<String, String> fallback;

    static {
        fallback = Translation.loadTranslationMap("en_us");
    }

    private Translation() {}

    public static String get(String key) {
        var value = translations.get(key);
        if (value == null) value = fallback.get(key);

        return value;
    }

    public static MutableComponent translatableWithFallback(String key) {
        return Component.translatableWithFallback(key, Translation.get(key));
    }

    public static MutableComponent translatableWithFallback(String key, Object... args) {
        return Component.translatableWithFallback(key, Translation.get(key), args);
    }

    public static void reloadTranslations() {
        translations = Translation.loadTranslationMap(SkinRestorer.getConfig().language());
    }

    private static ImmutableMap<String, String> loadTranslationMap(String lang) {
        var json = FileUtils.readResource(SkinRestorer.assetPath(String.format("lang/%s.json", lang)));

        var type = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> map = null;
        try {
            map = JsonUtils.fromJson(Objects.requireNonNull(json), type);
        } catch (Exception e) {
            SkinRestorer.LOGGER.error("Failed to load translation map", e);
        }

        if (map == null) return ImmutableMap.<String, String>builder().build();

        return ImmutableMap.copyOf(map);
    }
}
