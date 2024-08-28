package net.lionarius.skinrestorer.util;

import net.lionarius.skinrestorer.SkinRestorer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public final class WebUtils {
    
    private WebUtils() {}
    
    public static final String USER_AGENT;
    
    private static final HttpClient HTTP_CLIENT;
    
    static {
        USER_AGENT = String.format("SkinRestorer/%d", System.currentTimeMillis() % 65535);
        
        var builder = HttpClient.newBuilder();
        
        var proxy = SkinRestorer.getConfig().getProxy();
        proxy.ifPresent(value -> builder.proxy(ProxySelector.of(InetSocketAddress.createUnresolved(value.host(), value.port()))));
        
        try {
            builder.connectTimeout(Duration.of(SkinRestorer.getConfig().getRequestTimeout(), ChronoUnit.SECONDS));
        } catch (IllegalArgumentException e) {
            SkinRestorer.LOGGER.error("failed to set request timeout", e);
            builder.connectTimeout(Duration.of(10, ChronoUnit.SECONDS));
        }
        
        HTTP_CLIENT = builder.build();
    }
    
    public static HttpResponse<String> executeRequest(HttpRequest request) throws IOException {
        try {
            var modifiedRequest = HttpRequest.newBuilder(request, (name, value) -> true)
                    .header("User-Agent", WebUtils.USER_AGENT)
                    .build();
            
            final var response = WebUtils.HTTP_CLIENT.send(modifiedRequest, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 500)
                throw new IOException("server error " + response.statusCode());
            
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }
    
    public static void throwOnClientErrors(HttpResponse<?> response) {
        String message = null;
        switch (response.statusCode()) {
            case 400:
                message = "bad request";
                break;
            case 401:
                message = "unauthorized";
                break;
            case 403:
                message = "forbidden";
                break;
            case 404:
                message = "not found";
                break;
            case 405:
                message = "method not allowed";
                break;
            case 408:
                message = "request timeout";
                break;
            case 429:
                message = "too many requests";
                break;
        }
        
        if (message != null)
            throw new IllegalStateException(message);
    }
}
