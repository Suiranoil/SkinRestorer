package net.lionarius.skinrestorer.config.provider;

import com.google.gson.annotations.JsonAdapter;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.collection.CollectionProviderConfig;
import net.lionarius.skinrestorer.config.provider.custom.CustomProviderConfig;
import net.lionarius.skinrestorer.config.provider.custom.CustomProviderListDeserializer;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public final class ProvidersConfig implements GsonPostProcessable {
    public static final ProvidersConfig DEFAULT = new ProvidersConfig(
            new MojangProviderConfig(),
            new ElyByProviderConfig(),
            new MineskinProviderConfig(),
            new CollectionProviderConfig(),
            new ArrayList<>()
    );
    
    private MojangProviderConfig mojang;
    private ElyByProviderConfig ely_by;
    private MineskinProviderConfig mineskin;
    private CollectionProviderConfig collection;
    
    @JsonAdapter(CustomProviderListDeserializer.class)
    private List<CustomProviderConfig> custom;
    private transient List<CustomProviderConfig> validatedCustom;
    
    public ProvidersConfig(
            MojangProviderConfig mojang,
            ElyByProviderConfig ely_by,
            MineskinProviderConfig mineskin,
            CollectionProviderConfig collection,
            List<CustomProviderConfig> custom
    ) {
        this.mojang = mojang;
        this.ely_by = ely_by;
        this.mineskin = mineskin;
        this.collection = collection;
        
        this.custom = custom;
        this.validatedCustom = List.of();
    }
    
    public MojangProviderConfig mojang() {
        return this.mojang;
    }
    
    public ElyByProviderConfig ely_by() {
        return this.ely_by;
    }
    
    public MineskinProviderConfig mineskin() {
        return this.mineskin;
    }
    
    public CollectionProviderConfig collection() {
        return this.collection;
    }

    public List<CustomProviderConfig> custom() {
        return this.getValidatedCustom();
    }

    public <T extends CustomProviderConfig> Optional<T> findCustomByName(String name, Class<T> type) {
        return this.getValidatedCustom()
                .stream()
                .filter(config -> config.name().equals(name))
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst();
    }

    @Override
    public void gsonPostProcess() {
        if (this.mojang == null) {
            SkinRestorer.LOGGER.warn("Mojang provider config is null, using default");
            this.mojang = ProvidersConfig.DEFAULT.mojang();
        }
        
        if (this.ely_by == null) {
            SkinRestorer.LOGGER.warn("Ely.By provider config is null, using default");
            this.ely_by = ProvidersConfig.DEFAULT.ely_by();
        }
        
        if (this.mineskin == null) {
            SkinRestorer.LOGGER.warn("Mineskin provider config is null, using default");
            this.mineskin = ProvidersConfig.DEFAULT.mineskin();
        }
        
        if (this.collection == null) {
            SkinRestorer.LOGGER.warn("Collection provider config is null, using default");
            this.collection = ProvidersConfig.DEFAULT.collection();
        }

        if (this.custom == null) {
            SkinRestorer.LOGGER.warn("Custom providers config is null, using an empty list");
            this.custom = new ArrayList<>();
        }

        this.rebuildValidatedCustomProviders();
    }

    private List<CustomProviderConfig> getValidatedCustom() {
        if (this.validatedCustom == null)
            this.rebuildValidatedCustomProviders();

        if (this.validatedCustom == null)
            this.validatedCustom = List.of();

        return this.validatedCustom;
    }

    private void rebuildValidatedCustomProviders() {
        if (this.custom == null) {
            this.validatedCustom = List.of();
            return;
        }

        var seenNames = new HashSet<String>();
        var validated = new ArrayList<CustomProviderConfig>(this.custom.size());

        for (var config : this.custom) {
            if (config == null)
                continue;

            if (config.name().isEmpty()) {
                SkinRestorer.LOGGER.warn("Skipping custom provider with empty name");
                continue;
            }

            if (!seenNames.add(config.name())) {
                SkinRestorer.LOGGER.warn(
                        "Duplicate enabled custom provider name '{}' found; keeping the first one",
                        config.name()
                );
                continue;
            }

            validated.add(config);
        }

        this.validatedCustom = List.copyOf(validated);
    }
}
