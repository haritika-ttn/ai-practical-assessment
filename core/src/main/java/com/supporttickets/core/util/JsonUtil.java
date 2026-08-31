package com.supporttickets.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * JSON serialization helpers for servlet responses.
 */
public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    private JsonUtil() {
    }

    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    public static <T> T fromJson(InputStream inputStream, Class<T> type) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, type);
        } catch (JsonSyntaxException ex) {
            throw new IOException("Malformed JSON request body", ex);
        }
    }

    public static <T> T fromJson(InputStream inputStream, Type type) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, type);
        } catch (JsonSyntaxException ex) {
            throw new IOException("Malformed JSON request body", ex);
        }
    }
}
