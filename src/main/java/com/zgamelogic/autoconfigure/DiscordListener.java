package com.zgamelogic.autoconfigure;

import com.zgamelogic.annotations.DiscordMapping;
import com.zgamelogic.annotations.EventProperty;
import com.zgamelogic.helpers.Translator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.internal.utils.ClassWalker;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@Slf4j
public class DiscordListener implements EventListener {

    private final Map<Class<?>, List<ObjectMethod>> methods;
    private final List<ObjectField> botVars;
    private final List<Invalidation> invalidations;
    private boolean ready;

    public DiscordListener(){
        ready = false;
        methods = new HashMap<>();
        botVars = new LinkedList<>();
        invalidations = new LinkedList<>();
        // Autocomplete interactions
        invalidations.add((annotation, event) -> {
            try {
                CommandAutoCompleteInteractionEvent e = (CommandAutoCompleteInteractionEvent) event;
                if (!annotation.Id().isEmpty() && !annotation.Id().equals(e.getName())) return true;
                if (!annotation.SubId().isEmpty() && !annotation.SubId().equals(e.getSubcommandName())) return true;
                if (!annotation.FocusedOption().isEmpty() && !annotation.FocusedOption().equals(e.getFocusedOption().getName())) return true;
            } catch (Exception ignored) {}
            return false;
        });
        // interaction commands
        invalidations.add((annotation, event) -> {
            try {
                GenericCommandInteractionEvent e = (GenericCommandInteractionEvent) event;
                if(!annotation.Id().isEmpty() && !annotation.Id().equals(e.getName())) return true;
                if(!annotation.SubId().isEmpty() && !annotation.SubId().equals(e.getSubcommandName())) return true;
            } catch (Exception ignored) {}
            return false;
        });
        // Modals
        invalidations.add((annotation, event) -> {
            try {
                ModalInteractionEvent e = (ModalInteractionEvent) event;
                if(!annotation.Id().isEmpty() && !annotation.Id().equals(e.getModalId())) return true;
            } catch (Exception ignored) {}
            return false;
        });
        // Buttons
        invalidations.add((annotation, event) -> {
            try {
                ButtonInteractionEvent e = (ButtonInteractionEvent) event;
                if(!annotation.Id().isEmpty() && !annotation.Id().equals(e.getButton().getId())) return true;
            } catch (Exception ignored) {}
            return false;
        });
        // String select interaction
        invalidations.add((annotation, event) -> {
            try {
                StringSelectInteractionEvent e = (StringSelectInteractionEvent) event;
                if(!annotation.Id().isEmpty() && !annotation.Id().equals(e.getSelectMenu().getId())) return true;
                if(!annotation.FocusedOption().isEmpty() && !annotation.FocusedOption().equals(e.getInteraction().getSelectedOptions().get(0).getValue())) return true;
            } catch (Exception ignored) {}
            return false;
        });
        // Entity select interaction
        invalidations.add((annotation, event) -> {
            try {
                StringSelectInteractionEvent e = (StringSelectInteractionEvent) event;
                if(!annotation.Id().isEmpty() && !annotation.Id().equals(e.getSelectMenu().getId())) return true;
            } catch (Exception ignored) {}
            return false;
        });
    }

    public void addObjectMethod(Object object, Method method){
        List<Parameter> JDAParams = Arrays.stream(method.getParameters()).filter(parameter -> Event.class.isAssignableFrom(parameter.getType())).toList();
        if(JDAParams.size() != 1){
            log.error("Error when mapping method: {}", method);
            log.error("Discord mappings must have one JDA event parameter.");
            throw new RuntimeException("Discord mappings must have one JDA event parameter");
        }
        Class<?> clazz = JDAParams.get(0).getType();
        if(methods.containsKey(clazz)){
            methods.get(clazz).add(new ObjectMethod(object, method));
        } else {
            methods.put(clazz, new LinkedList<>(List.of(new ObjectMethod(object, method))));
        }
    }

    public void addReadyObjectField(Object object, Field field){
        botVars.add(new ObjectField(object, field));
    }

    @Override
    public void onEvent(GenericEvent event) {
        for (Class<?> clazz : ClassWalker.range(event.getClass(), GenericEvent.class)) {
            if(!ready && clazz == ReadyEvent.class){ // ready event for initialization to set all the bot vars
                ready = true;
                botVars.forEach(objectField -> {
                    try {
                        objectField.getField().setAccessible(true);
                        objectField.getField().set(objectField.getObject(), event.getJDA());
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
                botVars.clear();
            }
            if(methods.containsKey(clazz)){
                methods.get(clazz).forEach(objectMethod -> {
                    objectMethod.method.setAccessible(true);
                    try {
                        DiscordMapping annotation = objectMethod.method.getAnnotation(DiscordMapping.class);
                        for(Invalidation invalidation: invalidations) if(invalidation.isInvalid(annotation, event)) return;
                        List<Object> params = constructParameters(event, objectMethod.method);
                        objectMethod.method.invoke(objectMethod.object, params.toArray());
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    private List<Object> constructParameters(GenericEvent event, Method method){
        List<Object> parameters = new LinkedList<>();
        for(Parameter methodParam: method.getParameters()){
            if(Event.class.isAssignableFrom(methodParam.getType())){
                parameters.add(event);
                continue;
            }
            if(methodParam.isAnnotationPresent(EventProperty.class)){
                EventProperty annotation = methodParam.getAnnotation(EventProperty.class);
                String name = annotation.name().isEmpty() ? methodParam.getName() : annotation.name();
                if(event instanceof SlashCommandInteractionEvent slashEvent){
                    Object o = Translator.eventOptionToObject(slashEvent.getOption(name));
                    parameters.add(o);
                    continue;
                } else if(event instanceof ModalInteractionEvent modalEvent) {
                    parameters.add(modalEvent.getValue(name) == null ? null : Objects.requireNonNull(modalEvent.getValue(name)).getAsString());
                    continue;
                }
            }
            parameters.add(null);
        }
        return parameters;
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

    private interface Invalidation {
        boolean isInvalid(DiscordMapping annotation, GenericEvent event);
    }
}
