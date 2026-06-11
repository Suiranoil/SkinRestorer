package net.lionarius.skinrestorer.config;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;

import java.util.Optional;

public final class RequestConfig implements GsonPostProcessable {
    private String proxy = "";
    private transient Proxy parsedProxy = null;

    private long timeout = 10;

    private String userAgent = "";

    public Optional<Proxy> proxy() {
        return Optional.ofNullable(this.parsedProxy);
    }

    public long timeout() {
        return this.timeout;
    }

    public String userAgent() {
        return this.userAgent;
    }

    @Override
    public void gsonPostProcess() {
        if (this.proxy == null) {
            SkinRestorer.LOGGER.warn("Proxy config is null, defaulting to an empty string");
            this.proxy = "";
        }

        if (!this.proxy.isEmpty()) {
            try {
                this.parsedProxy = Proxy.parse(this.proxy);
            } catch (Exception e) {
                SkinRestorer.LOGGER.warn("Could not parse proxy config: {}", e.getMessage());
                this.parsedProxy = null;
            }
        }

        if (this.timeout <= 0) {
            SkinRestorer.LOGGER.warn("Request timeout config is less than or equal to 0, defaulting to 10");
            this.timeout = 10;
        }

        if (this.userAgent == null) {
            SkinRestorer.LOGGER.warn("User agent config is null, defaulting to an empty string");
            this.userAgent = "";
        }
    }
}
