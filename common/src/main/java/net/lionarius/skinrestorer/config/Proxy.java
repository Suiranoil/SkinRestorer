package net.lionarius.skinrestorer.config;

import org.jetbrains.annotations.NotNull;

public record Proxy(@NotNull String host, int port) {
    
    public static Proxy parse(@NotNull String proxy) {
        var colonIndex = proxy.lastIndexOf(':');
        if (colonIndex == -1)
            throw new IllegalArgumentException("no port in hostname");
        
        var port = Integer.parseInt(proxy.substring(colonIndex + 1));
        
        if (port < 0 || port > 0xFFFF)
            throw new IllegalArgumentException("port out of range: " + port);
        
        var host = proxy.substring(0, colonIndex);
        
        return new Proxy(host, port);
    }
}
