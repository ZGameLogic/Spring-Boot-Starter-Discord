package com.zgamelogic.autoconfigure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.internal.utils.ClassWalker;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@Slf4j
public class DiscordListener implements EventListener {

    private final Map<Class<?>, List<ObjectMethod>> methods;
    private final List<ObjectField> botVars;

    public DiscordListener(){
        methods = new HashMap<>();
        botVars = new LinkedList<>();
    }

    public void addObjectMethod(Object object, Method method){
        if(method.getParameterCount() != 1) {
            log.error("Error when mapping method: {}", method);
            log.error("Discord mappings must have one JDA event parameter.");
            throw new RuntimeException("Discord mappings must have one JDA event parameter");
        }
        Class<?> clazz = method.getParameters()[0].getType();
        if(methods.containsKey(clazz)){
            methods.get(clazz).add(new ObjectMethod(object, method));
        } else {
            methods.put(clazz, Collections.singletonList(new ObjectMethod(object, method)));
        }
    }

    public void addReadyObjectField(Object object, Field field){
        botVars.add(new ObjectField(object, field));
    }

    @Override
    public void onEvent(GenericEvent event) {
        for (Class<?> clazz : ClassWalker.range(event.getClass(), GenericEvent.class)) {
            if(clazz == ReadyEvent.class){
                botVars.forEach(objectField -> {
                    try {
                        objectField.getField().setAccessible(true);
                        objectField.getField().set(objectField.getObject(), event.getJDA());
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            if(methods.containsKey(clazz)){
                methods.get(clazz).forEach(objectMethod -> {
                    objectMethod.method.setAccessible(true);
                    try {
                        objectMethod.method.invoke(objectMethod.object, event);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    @Getter
    @AllArgsConstructor
    private static class ObjectMethod {
        private Object object;
        private Method method;
    }

    @Getter
    @AllArgsConstructor
    private static class ObjectField {
        private Object object;
        private Field field;
    }
}
