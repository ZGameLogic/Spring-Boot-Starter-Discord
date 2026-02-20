package com.zgamelogic.discord.components.exceptions;

import com.zgamelogic.discord.annotations.DiscordExceptionHandler;
import com.zgamelogic.discord.annotations.EventProperty;
import com.zgamelogic.discord.data.DiscordExceptionEvent;
import com.zgamelogic.discord.data.Model;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import static com.zgamelogic.discord.helpers.Translator.eventOptionToObject;
import static com.zgamelogic.discord.helpers.Translator.isClassValidToObject;

class DiscordExceptionApplicationListener implements GenericApplicationListener {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DiscordExceptionApplicationListener.class);
    private final ApplicationContext applicationContext;
    private final Method method;
    private final String beanName;
    private final List<Class<? extends Throwable>> supportedExceptions;

    DiscordExceptionApplicationListener(String beanName, Method method, DiscordExceptionHandler ann, ApplicationContext applicationContext) {
        this.beanName = beanName;
        this.method = method;
        this.applicationContext = applicationContext;
        supportedExceptions = List.of(ann.value());
    }

    @Override
    public void onApplicationEvent(@NotNull ApplicationEvent event) {
        if (!(event instanceof DiscordExceptionEvent e)) return;
        if(e.getSource().getClass() != method.getDeclaringClass()) return;
        if(!supportedExceptions.contains(e.getException().getClass())) return;
        try {
            Model model = new Model();
            Object bean = applicationContext.getBean(beanName);
            Object[] params = resolveParamsForExceptionMethod(method, e.getEvent(), model, e.getException());
            Object returned = method.invoke(bean, params);
        } catch (Exception ex) {
            log.error("Unable to call exception method", ex);
        }
    }

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        return DiscordExceptionEvent.class.isAssignableFrom(eventType.toClass());
    }

    private Object[] resolveParamsForExceptionMethod(Method method, GenericEvent event, Model model, Throwable throwable){
        return resolveParamsForArray(event, throwable, model, method.getParameters());
    }

    private Object[] resolveParamsForArray(GenericEvent event, Throwable throwable, Model model, Parameter...parameters){
        List<Object> params = new ArrayList<>();
        if (parameters == null) return params.toArray();
        for(Parameter parameter: parameters){
            if (event != null && parameter.getType().isAssignableFrom(event.getClass())) {
                params.add(event);
                continue;
            } else if (Event.class.isAssignableFrom(parameter.getType())){
                params.add(null);
                continue;
            } else if (Model.class.isAssignableFrom(parameter.getType())){
                params.add(model);
                continue;
            }
            if (throwable != null && parameter.getType().isAssignableFrom(throwable.getClass())) { // if it's the throwable
                params.add(throwable);
                continue;
            } else if(Throwable.class.isAssignableFrom(parameter.getType())){
                params.add(null);
                continue;
            }
            EventProperty eventProperty = parameter.getAnnotation(EventProperty.class);
            String name = eventProperty != null && !eventProperty.name().isEmpty() ? eventProperty.name() : parameter.getName();
            if(isClassValidToObject(parameter.getType())){ // event property
                params.add(extractOptionFromEvent(event, name));
            } else { // event record or event class
                Class<?> clazz = parameter.getType();
                if(clazz.getDeclaredConstructors().length == 0) continue;
                Constructor<?> con = clazz.getDeclaredConstructors()[0];
                con.setAccessible(true);
                Object obj;
                try {
                    obj = con.newInstance(resolveParamsForArray(event, throwable, model, con.getParameters()));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
                params.add(obj);
            }
        }
        return params.toArray();
    }

    private Object extractOptionFromEvent(GenericEvent event, String name){
        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            return eventOptionToObject(slashEvent.getOption(name));
        } else if (event instanceof CommandAutoCompleteInteractionEvent autoCompleteEvent) {
            return eventOptionToObject(autoCompleteEvent.getOption(name));
        } else if (event instanceof ModalInteractionEvent modalEvent) {
            if(modalEvent.getValue(name) == null) return null;
            return modalEvent.getValue(name).getAsString();
        }
        return null;
    }
}
