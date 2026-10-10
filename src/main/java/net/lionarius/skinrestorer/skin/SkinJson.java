package net.lionarius.skinrestorer.skin;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/** Strict, bounded JSON decoding compatible with the Gson bundled by Minecraft 1.7.10. */
public final class SkinJson {

    private static final int MAX_DEPTH = 32;
    private static final int MAX_CHARACTERS = 131072;

    private SkinJson() {}

    public static String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Response is not valid UTF-8", exception);
        }
    }

    public static JsonObject parseObject(String text) {
        if (text == null || text.isEmpty() || text.length() > MAX_CHARACTERS) {
            throw new IllegalArgumentException("JSON is empty or exceeds the size limit");
        }
        validateStringSyntax(text);
        // JsonParser in old Gson silently enables leniency. Reading tokens ourselves keeps
        // malformed JSON, duplicate properties and deeply nested documents out of persisted state.
        try (JsonReader reader = new JsonReader(new StringReader(text))) {
            reader.setLenient(false);
            JsonElement result = read(reader, 0);
            if (!result.isJsonObject() || reader.peek() != JsonToken.END_DOCUMENT) {
                throw new IllegalArgumentException("Expected a single JSON object");
            }
            return result.getAsJsonObject();
        } catch (IOException | NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid JSON document", exception);
        }
    }

    private static void validateStringSyntax(String text) {
        boolean quoted = false;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (!quoted) {
                if (character == '"') {
                    quoted = true;
                }
                continue;
            }
            if (character == '"') {
                quoted = false;
            } else if (character < 0x20) {
                throw new IllegalArgumentException("Unescaped control character in JSON string");
            } else if (character == '\\') {
                if (++index >= text.length()) {
                    throw new IllegalArgumentException("Unterminated JSON escape");
                }
                char escape = text.charAt(index);
                if (escape == 'u') {
                    for (int digit = 0; digit < 4; digit++) {
                        if (++index >= text.length() || "0123456789abcdefABCDEF".indexOf(text.charAt(index)) < 0) {
                            throw new IllegalArgumentException("Invalid JSON Unicode escape");
                        }
                    }
                } else if ("\"\\/bfnrt".indexOf(escape) < 0) {
                    throw new IllegalArgumentException("Invalid JSON string escape");
                }
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Unterminated JSON string");
        }
    }

    private static void validateUnicode(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isHighSurrogate(character)) {
                if (++index >= value.length() || !Character.isLowSurrogate(value.charAt(index))) {
                    throw new IllegalArgumentException("Unpaired surrogate in JSON string");
                }
            } else if (Character.isLowSurrogate(character)) {
                throw new IllegalArgumentException("Unpaired surrogate in JSON string");
            }
        }
    }

    public static String requireString(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive()
            || !element.getAsJsonPrimitive()
                .isString()) {
            throw new IllegalArgumentException("Expected string field: " + name);
        }
        return element.getAsString();
    }

    public static long requireLong(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive()
            || !element.getAsJsonPrimitive()
                .isNumber()) {
            throw new IllegalArgumentException("Expected integer field: " + name);
        }
        String value = element.getAsString();
        if (!value.matches("-?(0|[1-9][0-9]*)")) {
            throw new IllegalArgumentException("Expected integer field: " + name);
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Integer field is out of range: " + name, exception);
        }
    }

    private static JsonElement read(JsonReader reader, int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("JSON nesting exceeds the depth limit");
        }
        return switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                JsonObject object = new JsonObject();
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    validateUnicode(name);
                    if (object.has(name)) {
                        throw new IllegalArgumentException("Duplicate JSON field: " + name);
                    }
                    object.add(name, read(reader, depth + 1));
                }
                reader.endObject();
                yield object;
            }
            case BEGIN_ARRAY -> {
                JsonArray array = new JsonArray();
                reader.beginArray();
                while (reader.hasNext()) {
                    array.add(read(reader, depth + 1));
                }
                reader.endArray();
                yield array;
            }
            case STRING -> {
                String value = reader.nextString();
                validateUnicode(value);
                yield new JsonPrimitive(value);
            }
            case NUMBER -> {
                String value = reader.nextString();
                if (value.length() > 128 || !value.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")) {
                    throw new IllegalArgumentException("Invalid JSON number");
                }
                yield new JsonPrimitive(new BigDecimal(value));
            }
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> {
                reader.nextNull();
                yield JsonNull.INSTANCE;
            }
            default -> throw new IllegalArgumentException("Unexpected JSON token: " + reader.peek());
        };
    }
}
