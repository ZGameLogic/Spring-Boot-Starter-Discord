package com.zgamelogic.autoconfigure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.internal.utils.ClassWalker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

class DiscordListener implements EventListener {

    private final Map<Class<?>, List<ObjectMethod>> methods;

    public DiscordListener(){
        methods = new HashMap<>();
    }

    public void addObjectMethod(Object object, Method method){
        if(method.getParameterCount() != 1) throw new RuntimeException("Spring-JDA methods must have one parameter");
        Class<?> clazz = method.getParameters()[0].getType();
        if(methods.containsKey(clazz)){
            methods.get(clazz).add(new ObjectMethod(object, method));
        } else {
            methods.put(clazz, Collections.singletonList(new ObjectMethod(object, method)));
        }
    }

    @Override
    public void onEvent(GenericEvent event) {
        for (Class<?> clazz : ClassWalker.range(event.getClass(), GenericEvent.class)) {
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
}
