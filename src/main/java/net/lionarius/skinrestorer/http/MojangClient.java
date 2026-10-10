package net.lionarius.skinrestorer.http;

import static net.lionarius.skinrestorer.http.MojangException.Reason;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import net.lionarius.skinrestorer.skin.SkinJson;
import net.lionarius.skinrestorer.skin.SkinTexture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Bounded, asynchronous official Mojang lookup. No authentication or live-profile mutation. */
public final class MojangClient implements AutoCloseable {

    private static final int MAX_RESPONSE_BYTES = 131072;
    private static final String PROFILE_API = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SESSION_API = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private final Object lock = new Object();
    private final Duration requestTimeout;
    private final long positiveTtlNanos;
    private final int cacheCapacity;
    private final HttpClient http;
    private final ThreadPoolExecutor workers;
    private final Map<String, CompletableFuture<SkinTexture>> inFlight = new LinkedHashMap<>();
    private final LinkedHashMap<String, CachedResult> cache = new LinkedHashMap<>(16, 0.75f, true);
    private Instant retryNotBefore = Instant.MIN;
    private boolean closed;

    public MojangClient(Duration requestTimeout, int concurrency, int queueCapacity, long cacheTtlSeconds) {
        this(
            requestTimeout,
            concurrency,
            queueCapacity,
            cacheTtlSeconds,
            newHttpClient(requestTimeout, concurrency, queueCapacity, cacheTtlSeconds));
    }

    // Enables deterministic transport tests without exposing alternate production endpoints.
    MojangClient(Duration requestTimeout, int concurrency, int queueCapacity, long cacheTtlSeconds, HttpClient http) {
        this.requestTimeout = validateTimeout(requestTimeout);
        validateLimits(concurrency, queueCapacity, cacheTtlSeconds);
        this.positiveTtlNanos = Math.min(TimeUnit.SECONDS.toNanos(cacheTtlSeconds), Long.MAX_VALUE / 4);
        this.cacheCapacity = Math.max(64, Math.min(1024, (concurrency + queueCapacity) * 4));
        this.http = Objects.requireNonNull(http, "http");
        BlockingQueue<Runnable> queue = queueCapacity == 0 ? new SynchronousQueue<>()
            : new ArrayBlockingQueue<>(queueCapacity);
        AtomicInteger sequence = new AtomicInteger();
        this.workers = new ThreadPoolExecutor(concurrency, concurrency, 0L, TimeUnit.MILLISECONDS, queue, runnable -> {
            Thread thread = new Thread(runnable, "SkinRestorer-Mojang-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy());
    }

    /** Invalid input is reported through the returned future, just like transport failures. */
    public CompletableFuture<SkinTexture> fetch(String username, boolean force) {
        try {
            validateUsername(username);
        } catch (IllegalArgumentException exception) {
            return CompletableFuture
                .failedFuture(new MojangException(Reason.INVALID_USERNAME, exception.getMessage(), exception));
        }
        String key = username.toLowerCase(Locale.ROOT);
        synchronized (lock) {
            if (closed) {
                return CompletableFuture
                    .failedFuture(new MojangException(Reason.CLOSED, "Mojang skin client is closed"));
            }
            CompletableFuture<SkinTexture> existing = inFlight.get(key);
            if (existing != null) {
                return detached(existing);
            }
            CachedResult cached = cache.get(key);
            if (cached != null && cached.expiresAtNanos() - System.nanoTime() <= 0) {
                cache.remove(key);
                cached = null;
            }
            if (!force && cached != null) {
                return cached.error() == null ? CompletableFuture.completedFuture(cached.texture())
                    : CompletableFuture.failedFuture(cached.error());
            }
            MojangException rateLimit = currentRateLimit();
            if (rateLimit != null) {
                return CompletableFuture.failedFuture(rateLimit);
            }
            CompletableFuture<SkinTexture> result = new CompletableFuture<>();
            inFlight.put(key, result);
            try {
                workers.execute(() -> lookup(key, username, result));
            } catch (RejectedExecutionException exception) {
                inFlight.remove(key);
                result.completeExceptionally(
                    new MojangException(Reason.QUEUE_FULL, "Mojang request queue is full; try again later", exception));
            }
            // Caller cancellation is isolated from the shared request and other players.
            return detached(result);
        }
    }

    public static String validateUsername(String username) {
        if (username == null || !username.matches("[A-Za-z0-9_]{1,16}")) {
            throw new IllegalArgumentException(
                "Mojang username must contain 1-16 ASCII letters, digits or underscores");
        }
        return username;
    }

    /** Initiates cancellation without waiting on workers, HTTP selectors, or a game tick. */
    @Override
    public void close() {
        List<CompletableFuture<SkinTexture>> pending;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            pending = new ArrayList<>(inFlight.values());
            inFlight.clear();
            cache.clear();
        }
        workers.shutdownNow();
        http.shutdownNow();
        MojangException failure = new MojangException(Reason.CLOSED, "Mojang skin client is closed");
        pending.forEach(future -> future.completeExceptionally(failure));
    }

    private void lookup(String key, String username, CompletableFuture<SkinTexture> result) {
        SkinTexture texture = null;
        MojangException error = null;
        try {
            JsonObject account = requestJson(URI.create(PROFILE_API + username));
            String id = requireProfileId(account);
            if (!validateUsername(SkinJson.requireString(account, "name")).equalsIgnoreCase(username)) {
                throw new MojangException(Reason.INVALID_RESPONSE, "Mojang returned a different username");
            }
            JsonObject profile = requestJson(URI.create(SESSION_API + id + "?unsigned=false"));
            if (!id.equalsIgnoreCase(requireProfileId(profile))) {
                throw new MojangException(
                    Reason.INVALID_RESPONSE,
                    "Mojang session profile UUID does not match the lookup");
            }
            JsonElement properties = profile.get("properties");
            if (properties == null || !properties.isJsonArray()
                || properties.getAsJsonArray()
                    .size() == 0) {
                throw new MojangException(Reason.NO_SKIN, "Mojang account has no signed skin texture");
            }
            boolean hasTextures = false;
            for (JsonElement property : properties.getAsJsonArray()) {
                if (property.isJsonObject()
                    && "textures".equals(SkinJson.requireString(property.getAsJsonObject(), "name"))) {
                    hasTextures = true;
                }
            }
            if (!hasTextures) {
                throw new MojangException(Reason.NO_SKIN, "Mojang account has no signed skin texture");
            }
            texture = SkinTexture.parseMojangProfile(profile);
        } catch (MojangException exception) {
            error = exception;
        } catch (IllegalArgumentException | NullPointerException exception) {
            error = new MojangException(Reason.INVALID_RESPONSE, "Mojang returned an invalid skin profile", exception);
        } catch (RuntimeException exception) {
            error = new MojangException(Reason.NETWORK, "Mojang lookup failed", exception);
        }
        synchronized (lock) {
            if (inFlight.get(key) != result || closed) {
                return;
            }
            inFlight.remove(key);
            long ttl = error == null ? positiveTtlNanos : negativeTtlNanos(error);
            if (ttl > 0) {
                cache.put(key, new CachedResult(texture, error, System.nanoTime() + ttl));
                while (cache.size() > cacheCapacity) {
                    cache.remove(
                        cache.keySet()
                            .iterator()
                            .next());
                }
            }
        }
        if (error == null) {
            result.complete(texture);
        } else {
            result.completeExceptionally(error);
        }
    }

    private JsonObject requestJson(URI uri) {
        synchronized (lock) {
            if (closed) {
                throw new MojangException(Reason.CLOSED, "Mojang skin client is closed");
            }
            MojangException rateLimit = currentRateLimit();
            if (rateLimit != null) {
                throw rateLimit;
            }
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(requestTimeout)
            .header("Accept", "application/json")
            .header("User-Agent", "SkinRestorer-GTNH/1.7.10")
            .GET()
            .build();
        CompletableFuture<HttpResponse<byte[]>> pending = http.sendAsync(request, info -> new LimitedBodySubscriber());
        HttpResponse<byte[]> response;
        try {
            // Covers slow bodies as well as connection/response headers; ofInputStream would not.
            response = pending.get(requestTimeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException exception) {
            pending.cancel(true);
            throw new MojangException(Reason.TIMEOUT, "Mojang request timed out", exception);
        } catch (InterruptedException exception) {
            pending.cancel(true);
            Thread.currentThread()
                .interrupt();
            throw new MojangException(Reason.CLOSED, "Mojang request was cancelled during shutdown", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            while (cause instanceof CompletionException && cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (cause instanceof MojangException failure) {
                throw failure;
            }
            throw new MojangException(
                cause instanceof HttpTimeoutException ? Reason.TIMEOUT : Reason.NETWORK,
                cause instanceof HttpTimeoutException ? "Mojang request timed out" : "Cannot reach Mojang skin service",
                cause);
        }
        int status = response.statusCode();
        if (status == 429) {
            Duration retry = retryAfter(response);
            synchronized (lock) {
                Instant deadline;
                try {
                    deadline = Instant.now()
                        .plus(retry);
                } catch (java.time.DateTimeException | ArithmeticException exception) {
                    deadline = Instant.MAX;
                }
                if (deadline.isAfter(retryNotBefore)) {
                    retryNotBefore = deadline;
                }
            }
            throw new MojangException(
                Reason.RATE_LIMITED,
                "Mojang rate limit reached; try again after the reported delay",
                status,
                retry,
                null);
        }
        if (status == 404 || status == 204) {
            throw new MojangException(
                Reason.NOT_FOUND,
                "Mojang account or skin profile was not found",
                status,
                Duration.ZERO,
                null);
        }
        if (status != 200) {
            throw new MojangException(
                Reason.HTTP_ERROR,
                "Mojang skin service returned HTTP " + status,
                status,
                Duration.ZERO,
                null);
        }
        try {
            return SkinJson.parseObject(SkinJson.decodeUtf8(response.body()));
        } catch (IllegalArgumentException exception) {
            throw new MojangException(
                Reason.INVALID_RESPONSE,
                "Mojang returned malformed or non-UTF-8 JSON",
                exception);
        }
    }

    private MojangException currentRateLimit() {
        Instant now = Instant.now();
        if (now.isBefore(retryNotBefore)) {
            return new MojangException(
                Reason.RATE_LIMITED,
                "Mojang rate limit is still active; try again later",
                429,
                Duration.between(now, retryNotBefore),
                null);
        }
        return null;
    }

    private long negativeTtlNanos(MojangException failure) {
        if (positiveTtlNanos == 0 || failure.reason() == Reason.CLOSED || failure.reason() == Reason.RATE_LIMITED) {
            return 0;
        }
        long seconds = failure.reason() == Reason.NOT_FOUND || failure.reason() == Reason.NO_SKIN ? 60 : 5;
        return Math.min(positiveTtlNanos, TimeUnit.SECONDS.toNanos(seconds));
    }

    private static Duration retryAfter(HttpResponse<?> response) {
        String header = response.headers()
            .firstValue("Retry-After")
            .orElse("")
            .trim();
        try {
            if (header.matches("[0-9]{1,19}")) {
                return Duration.ofSeconds(Long.parseLong(header));
            }
            Instant deadline = DateTimeFormatter.RFC_1123_DATE_TIME.parse(header, Instant::from);
            Duration delay = Duration.between(Instant.now(), deadline);
            return delay.isNegative() ? Duration.ZERO : delay;
        } catch (DateTimeParseException | NumberFormatException exception) {
            return Duration.ofSeconds(60);
        }
    }

    private static String requireProfileId(JsonObject profile) {
        String id = SkinJson.requireString(profile, "id");
        if (!id.matches("[a-fA-F0-9]{32}")) {
            throw new IllegalArgumentException("Mojang profile ID must be 32 hexadecimal digits");
        }
        return id.toLowerCase(Locale.ROOT);
    }

    private static Duration validateTimeout(Duration timeout) {
        Objects.requireNonNull(timeout, "requestTimeout");
        if (timeout.isNegative() || timeout.isZero() || timeout.compareTo(Duration.ofMinutes(5)) > 0) {
            throw new IllegalArgumentException("Mojang request timeout must be positive and at most five minutes");
        }
        return timeout;
    }

    private static void validateLimits(int concurrency, int queueCapacity, long cacheTtlSeconds) {
        if (concurrency < 1 || concurrency > 32 || queueCapacity < 0 || queueCapacity > 4096 || cacheTtlSeconds < 0) {
            throw new IllegalArgumentException(
                "Concurrency must be 1-32, queue capacity 0-4096 and cache TTL non-negative");
        }
    }

    private static HttpClient newHttpClient(Duration timeout, int concurrency, int queueCapacity,
        long cacheTtlSeconds) {
        validateLimits(concurrency, queueCapacity, cacheTtlSeconds);
        return HttpClient.newBuilder()
            .connectTimeout(validateTimeout(timeout))
            .followRedirects(HttpClient.Redirect.NEVER)
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    }

    private static CompletableFuture<SkinTexture> detached(CompletableFuture<SkinTexture> shared) {
        return shared.thenApply(texture -> texture);
    }

    private record CachedResult(SkinTexture texture, MojangException error, long expiresAtNanos) {}

    private static final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {

        private final CompletableFuture<byte[]> body = new CompletableFuture<>();
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private Flow.Subscription subscription;

        @Override
        public CompletionStage<byte[]> getBody() {
            return body;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (this.subscription != null) {
                subscription.cancel();
                return;
            }
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            if (body.isDone()) {
                return;
            }
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > MAX_RESPONSE_BYTES - bytes.size()) {
                    subscription.cancel();
                    body.completeExceptionally(
                        new MojangException(Reason.INVALID_RESPONSE, "Mojang response exceeds the size limit"));
                    return;
                }
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                bytes.write(chunk, 0, chunk.length);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            body.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            body.complete(bytes.toByteArray());
        }
    }
}
