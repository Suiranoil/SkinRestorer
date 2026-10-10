package net.lionarius.skinrestorer.skin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Synchronous disk operations; the owner must call these methods away from the game thread. */
public final class SkinStore implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger("SkinRestorer/SkinStore");
    private static final int MAX_STATE_BYTES = 65536;
    private final Path root;
    private boolean closed;

    public SkinStore(Path root) throws IOException {
        this.root = Objects.requireNonNull(root, "root")
            .toAbsolutePath()
            .normalize();
        Files.createDirectories(this.root);
        if (!Files.isDirectory(this.root, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Skin-state root must be a real directory: " + this.root);
        }
    }

    public synchronized Optional<SkinState> read(UUID id) throws IOException {
        ensureOpen();
        Path file = statePath(id);
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            LOGGER.warn("Ignoring skin state that is not a regular file: {}", file);
            return Optional.empty();
        }
        try (InputStream input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = input.readNBytes(MAX_STATE_BYTES + 1);
            if (bytes.length > MAX_STATE_BYTES) {
                throw new IllegalArgumentException("Skin-state file exceeds its size limit");
            }
            SkinState state = SkinState.fromJson(SkinJson.parseObject(SkinJson.decodeUtf8(bytes)));
            if (!id.equals(state.id())) {
                throw new IllegalArgumentException("Skin-state UUID does not match its filename");
            }
            return Optional.of(state);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Ignoring corrupt skin state in {}: {}", file, exception.getMessage());
            return Optional.empty();
        } catch (IOException exception) {
            LOGGER.warn("Unable to read skin state in {}", file, exception);
            throw exception;
        }
    }

    public synchronized void write(SkinState state) throws IOException {
        ensureOpen();
        Objects.requireNonNull(state, "state");
        Path target = statePath(state.id());
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
            && !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Refusing to replace non-regular skin state: " + target);
        }
        byte[] bytes = (state.toJson()
            .toString() + "\n").getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STATE_BYTES) {
            throw new IOException("Skin state exceeds its size limit");
        }
        Path temporary = Files.createTempFile(root, state.id() + ".", ".tmp");
        try {
            try (FileChannel output = FileChannel.open(
                temporary,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
                LinkOption.NOFOLLOW_LINKS)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    output.write(buffer);
                }
                output.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                LOGGER.debug("Atomic skin-state replacement unavailable in {}; using replacement move", root);
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.warn("Unable to persist skin state for {}", state.id(), exception);
            throw exception;
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException exception) {
                LOGGER.warn("Unable to remove temporary skin-state file {}", temporary, exception);
            }
        }
    }

    /** Known persisted identities only; playerdata lookup and name-ambiguity handling belong to the owner. */
    public synchronized Map<UUID, String> loadKnownPlayers() throws IOException {
        ensureOpen();
        Map<UUID, String> players = new LinkedHashMap<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(root, "*.json")) {
            for (Path file : files) {
                String filename = file.getFileName()
                    .toString();
                String rawId = filename.substring(0, filename.length() - 5);
                UUID id;
                try {
                    id = UUID.fromString(rawId);
                    if (!id.toString()
                        .equals(rawId)) {
                        throw new IllegalArgumentException("Non-canonical UUID filename");
                    }
                } catch (IllegalArgumentException exception) {
                    LOGGER.warn("Ignoring skin state with invalid UUID filename: {}", file);
                    continue;
                }
                try {
                    read(id).ifPresent(state -> players.put(id, state.playerName()));
                } catch (IOException exception) {
                    // One unreadable file must not hide all other known persisted players.
                    LOGGER.warn("Skipping unreadable persisted identity {}", id);
                }
            }
        }
        return Map.copyOf(players);
    }

    /** Writes are synchronous and forced before return; there is no deferred data to flush. */
    public synchronized void flush() throws IOException {
        ensureOpen();
    }

    @Override
    public synchronized void close() {
        closed = true;
    }

    private Path statePath(UUID id) {
        Objects.requireNonNull(id, "id");
        Path path = root.resolve(id.toString() + ".json")
            .normalize();
        if (!root.equals(path.getParent())) {
            throw new IllegalArgumentException("Skin-state path escapes its world directory");
        }
        return path;
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Skin-state store is closed");
        }
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Skin-state directory is unavailable: " + root);
        }
    }
}
