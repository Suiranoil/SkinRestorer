package net.lionarius.skinrestorer.skin.provider;

import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class SkinProviderRegistry {
    
    private final Map<String, Entry> registry = new HashMap<>();
    
    public SkinProvider get(String name) {
        var entry = this.registry.get(name);
        if (entry == null)
            return null;
        
        return entry.provider;
    }
    
    public Collection<Pair<String, SkinProvider>> getProviders() {
        return this.registry
                .entrySet()
                .stream()
                .map(entry -> Pair.of(entry.getKey(), entry.getValue().provider))
                .toList();
    }
    
    public Collection<Pair<String, SkinProvider>> getPublicProviders() {
        return this.registry
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().isPublic)
                .map(entry -> Pair.of(entry.getKey(), entry.getValue().provider))
                .toList();
    }
    
    public void register(@NotNull String name, @NotNull SkinProvider provider) {
        this.register(name, provider, true);
    }
    
    public void register(@NotNull String name, @NotNull SkinProvider provider, boolean isPublic) {
        if (this.registry.containsKey(name))
            return;
        
        this.registry.put(name, new Entry(provider, isPublic));
    }
    
    private record Entry(SkinProvider provider, boolean isPublic) {
    }
}
