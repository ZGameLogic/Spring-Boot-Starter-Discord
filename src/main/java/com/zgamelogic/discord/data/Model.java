package com.zgamelogic.discord.data;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Model {
    private final Map<String, Object> data;

    public Model() {
        this.data = new HashMap<>();
    }

    public void addContext(String key, Object value) {
        data.put(key, value);
    }

    public String resolveKey(String key) throws NoSuchFieldException, IllegalAccessException {
        if(key.contains(".")){
            String[] parts = key.split("\\.");
            String object = parts[0];
            String rest = String.join(".", Arrays.copyOfRange(parts, 1, parts.length));
            return resolveKey(rest, data.get(object)).toString();
        }
        return data.get(key).toString();
    }

    private Object resolveKey(String key, Object object) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(key);
        field.setAccessible(true);
        Object grabbed = field.get(object);
        if(key.contains(".")){
            String[] parts = key.split("\\.");
            String rest = String.join(".", Arrays.copyOfRange(parts, 1, parts.length));
            return resolveKey(rest, grabbed);
        }
        return grabbed;
    }
}
