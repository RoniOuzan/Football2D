package com.football.util.json;

import com.football.util.math.geometry.Translation2d;
import com.football.util.serialization.MapSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.Map;

public class JsonUtil {
    public static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(
                    new TypeToken<Map<Translation2d, Double>>(){}.getType(),
                    new MapSerializer())
            .create();

    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    public static <T> T fromJson(String json, Class<T> tClass) {
        return gson.fromJson(json, tClass);
    }
}
