package com.zgamelogic.discord.components.events;

import com.zgamelogic.discord.annotations.EventProperty;
import com.zgamelogic.discord.annotations.mappings.*;
import com.zgamelogic.discord.data.DiscordEvent;
import com.zgamelogic.discord.data.DiscordExceptionEvent;
import com.zgamelogic.discord.data.Model;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import static com.zgamelogic.discord.helpers.Translator.eventOptionToObject;
import static com.zgamelogic.discord.helpers.Translator.isClassValidToObject;

class DiscordEventApplicationListener implements GenericApplicationListener {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DiscordEventApplicationListener.class);
    private final ApplicationContext applicationContext;
    private final Method method;
    private final String methodKey;
    private final String beanName;

    DiscordEventApplicationListener(String beanName, Method method, Annotation ann, ApplicationContext applicationContext) {
        this.beanName = beanName;
        this.method = method;
        methodKey = generateKeyFromMethod(ann);
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(@NotNull ApplicationEvent event) {
        if (!(event instanceof DiscordEvent e)) return;
        if (!generateKeyFromEvent(e.getEvent()).equals(methodKey)) return;
        Model model = new Model();
        Object bean = applicationContext.getBean(beanName);
        Object[] params = resolveParamsForControllerMethod(method, e.getEvent(), model);
        try {
            Object returned = method.invoke(bean, params);
        } catch (InvocationTargetException ex) {
            applicationContext.publishEvent(new DiscordExceptionEvent(bean, e.getEvent(), ex.getTargetException()));
        } catch (IllegalAccessException ex){
            log.error("Unable to call event method", ex);
        }
    }

    private String generateKeyFromMethod(Annotation mapping){
        Class<?> clazz = null;
        String id = "";
        String subId = "";
        String groupName = "";
        String focusedOption = "";

        if(mapping instanceof SlashCommandMapping slashCommandMapping){
            clazz = SlashCommandInteractionEvent.class;
            id = AnnotatedElementUtils.findMergedAnnotation(method, SlashCommandMapping.class).id();
            subId = slashCommandMapping.sub();
            groupName = slashCommandMapping.group();
        } else if(mapping instanceof SlashCommandAutocompleteMapping slashCommandMapping){
            clazz = CommandAutoCompleteInteractionEvent.class;
            id = AnnotatedElementUtils.findMergedAnnotation(method, SlashCommandAutocompleteMapping.class).id();
            subId = slashCommandMapping.sub();
            groupName = slashCommandMapping.group();
            focusedOption = slashCommandMapping.focused();
        } else if(mapping instanceof MessageContextMapping){
            clazz = MessageContextInteractionEvent.class;
            id = AnnotatedElementUtils.findMergedAnnotation(method, MessageContextMapping.class).id();
        } else if (mapping instanceof ButtonMapping) {
            clazz = ButtonInteractionEvent.class;
            id = AnnotatedElementUtils.findMergedAnnotation(method, ButtonMapping.class).id();
        } else if (mapping instanceof StringSelectMapping) {
            clazz = StringSelectInteractionEvent.class;
            id = AnnotatedElementUtils.findMergedAnnotation(method, StringSelectMapping.class).id();
        } else if (mapping instanceof EntitySelectMapping) {
            clazz = EntitySelectInteractionEvent.class;
            id = AnnotatedElementUtils.findMergedAnnotation(method, EntitySelectMapping.class).id();
        } else if (mapping instanceof ModalMapping) {
            clazz = ModalInteractionEvent.class;
            id = AnnotatedElementUtils.findMergedAnnotation(method, ModalMapping.class).id();
        } else if (mapping instanceof GenericCommandMapping discordMapping){
            clazz = discordMapping.event() == Event.class ? null : discordMapping.event();
        }

        return String.format(
            "%s:%s:%s:%s:%s",
            clazz.getSimpleName(),
            id,
            subId,
            groupName,
            focusedOption
        );
    }

    private String generateKeyFromEvent(GenericEvent genericEvent) {
        if (genericEvent instanceof CommandAutoCompleteInteractionEvent event) {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                event.getName(),
                event.getSubcommandName() != null ? event.getSubcommandName() : "",
                event.getSubcommandGroup() != null ? event.getSubcommandGroup() : "",
                event.getFocusedOption().getName()
            );
        } else if (genericEvent instanceof GenericCommandInteractionEvent event) {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                event.getName(),
                event.getSubcommandName() != null ? event.getSubcommandName() : "",
                event.getSubcommandGroup() != null ? event.getSubcommandGroup() : "",
                ""
            );
        } else if (genericEvent instanceof ModalInteractionEvent event) {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                event.getModalId(),
                "",
                "",
                ""
            );
        } else if (genericEvent instanceof ButtonInteractionEvent event) {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                event.getButton().getCustomId(),
                "",
                "",
                ""
            );
        } else if (genericEvent instanceof GenericSelectMenuInteractionEvent<?, ?> event) {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                event.getSelectMenu().getCustomId(),
                "",
                "",
                ""
            );
        } else {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                "",
                "",
                "",
                ""
            );
        }
    }

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        return DiscordEvent.class.isAssignableFrom(eventType.toClass());
    }

    private Object[] resolveParamsForControllerMethod(Method method, GenericEvent event, Model model){
        return resolveParamsForArray(event, null, model, method.getParameters());
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
