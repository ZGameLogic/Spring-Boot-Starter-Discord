package com.zgamelogic.discord.helpers;

import com.zgamelogic.discord.annotations.EventProperty;
import com.zgamelogic.discord.services.ironwood.Model;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public abstract class Mapper {
    public static Object[] resolveParamsForArray(GenericEvent event, Throwable throwable, Model model, Parameter... parameters){
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
            EventProperty eventProperty = AnnotatedElementUtils.findMergedAnnotation(parameter, EventProperty.class);

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

    private static Object extractOptionFromEvent(GenericEvent event, String name){
        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            return eventOptionToObject(slashEvent.getOption(name));
        } else if (event instanceof CommandAutoCompleteInteractionEvent autoCompleteEvent) {
            return eventOptionToObject(autoCompleteEvent.getOption(name));
        } else if (event instanceof ModalInteractionEvent modalEvent) {
            ModalMapping mapping = modalEvent.getValue(name);
            if(mapping == null) return null;
            return switch(mapping.getType()){
                case TEXT_INPUT -> mapping.getAsString();
                case STRING_SELECT -> {
                   List<String> list = mapping.getAsStringList();
                   if(list.size() == 1) yield list.getFirst();
                   yield list;
                }
                case MENTIONABLE_SELECT -> mapping.getAsMentions();
                default -> null;
            };
        }
        return null;
    }

    private static boolean isClassValidToObject(Class<?> clazz){
        return List.of(
                String.class,
                Integer.class,
                int.class,
                Boolean.class,
                boolean.class,
                User.class,
                Channel.class,
                Role.class,
                IMentionable.class,
                Mentions.class,
                Message.Attachment.class
        ).contains(clazz);
    }

    private static Object eventOptionToObject(OptionMapping mapping){
        if(mapping == null) return null;
        return switch(mapping.getType()){
            case STRING -> mapping.getAsString();
            case INTEGER -> mapping.getAsInt();
            case BOOLEAN -> mapping.getAsBoolean();
            case USER -> mapping.getAsUser();
            case CHANNEL -> mapping.getAsChannel();
            case ROLE -> mapping.getAsRole();
            case MENTIONABLE -> mapping.getAsMentionable();
            case ATTACHMENT -> mapping.getAsAttachment();
            default -> null;
        };
    }
}
