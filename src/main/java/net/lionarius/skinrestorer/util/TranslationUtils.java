package net.lionarius.skinrestorer.util;

import net.lionarius.skinrestorer.SkinRestorer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class TranslationUtils {
	public static class Translation {
		public String skinActionAffectedProfile = "Skin has been saved for %s";
		public String skinActionAffectedPlayer = "Apply live skin changes for %s";
		public String skinActionFailed = "Failed to set skin";
		public String skinActionOk = "Skin changed";
	}
	public static Translation translation = new Translation();
	static {
		Path path = Path.of("config", "skinrestorer", "translation.json");
		if (Files.exists(path)) {
			try {
				translation = JsonUtils.fromJson(Objects.requireNonNull(FileUtils.readFile(path.toFile())), Translation.class);
			} catch (Exception ex) {
				SkinRestorer.LOGGER.error("Failed to load translation", ex);
			}
		}
	}
}
