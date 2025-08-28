package com.zgamelogic.discord.data;

import lombok.Getter;
import net.dv8tion.jda.api.utils.FileUpload;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Model {
    private final Map<String, Object> data;

    @Getter
    private final List<FileUpload> fileUploads;

    public Model() {
        this.data = new HashMap<>();
        this.fileUploads = new ArrayList<>();
    }

    public void addFileUpload(FileUpload fileUpload) {
        fileUploads.add(fileUpload);
    }

    public void addContext(String key, Object value) {
        data.put(key, value);
    }

    public String resolveKey(String key) throws NoSuchFieldException, IllegalAccessException {
        Object value = resolveKeyRecursive(key, data);
        return value != null ? value.toString() : "";
    }

    private Object resolveKeyRecursive(String key, Object context) throws NoSuchFieldException, IllegalAccessException {
        if (key == null || context == null) return null;
        Pattern pattern = Pattern.compile("([a-zA-Z0-9_]+)(\\[(\\d+)])?");
        String[] parts = key.split("\\.");
        Object current = context;

        for (String part : parts) {
            Matcher matcher = pattern.matcher(part);
            if (matcher.matches()) {
                String fieldName = matcher.group(1);
                String indexStr = matcher.group(3);

                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(fieldName);
                } else {
                    Field field = current.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    current = field.get(current);
                }

                if (indexStr != null && current instanceof List) {
                    int idx = Integer.parseInt(indexStr);
                    current = ((List<?>) current).get(idx);
                }
            }
        }
        return current;
    }

    public Collection<?> resolveCollection(String key) {
        Object value;
        try {
            value = resolveKeyRecursive(key, data);
        } catch (Exception e) {
            return new ArrayList<>();
        }
        return value instanceof Collection<?> ? (Collection<?>) value : new ArrayList<>();
    }
}
