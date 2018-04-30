package com.deshaion.transfertest.util;

import io.vertx.core.json.JsonObject;

import java.util.Map;

public class JsonUtils {
    public static JsonObject keysToUpperCase(JsonObject json) {
        JsonObject newJson = new JsonObject();

        for (Map.Entry<String, Object> entry : json) {
            newJson.put(entry.getKey().toUpperCase(), entry.getValue());
        }

        return newJson;
    }
}
