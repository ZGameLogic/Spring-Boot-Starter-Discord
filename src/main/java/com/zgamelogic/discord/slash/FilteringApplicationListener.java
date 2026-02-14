package com.zgamelogic.discord.slash;

import com.zgamelogic.discord.annotations.EventProperty;
import com.zgamelogic.discord.data.Model;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import static com.zgamelogic.discord.helpers.Translator.eventOptionToObject;
import static com.zgamelogic.discord.helpers.Translator.isClassValidToObject;

@Slf4j
class FilteringApplicationListener implements GenericApplicationListener {

    private final ApplicationContext applicationContext;
    private final Method method;
    private final String methodKey;
    private final String beanName;

    FilteringApplicationListener(String beanName, Method method, Annotation ann, ApplicationContext applicationContext) {
        this.beanName = beanName;
        this.method = method;
        methodKey = generateKeyFromMethod(ann);
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(@NotNull ApplicationEvent event) {
        if (!(event instanceof DiscordEvent e)) return;
        if (!generateKeyFromEvent(e.getEvent()).equals(methodKey)) return;
        try {
            Model model = new Model();
            Object bean = applicationContext.getBean(beanName);
            Object[] params = resolveParamsForControllerMethod(method, e.getEvent(), model);
            Object returned = method.invoke(bean, params);
            System.out.println(returned);
        } catch (Exception ex) {
            log.error("nope", ex);
        }
    }

    // TODO gotta change this to be more flexible with the annotation...
    private String generateKeyFromMethod(Annotation mapping){
        Class<?> clazz = null;
        String id = "";
        String subId = "";
        String groupName = "";
        String focusedOption = "";

        // TODO include other command mappings for the different annotations
        if(mapping instanceof SlashCommandMapping slashCommandMapping){
            clazz = SlashCommandInteractionEvent.class;
            id = slashCommandMapping.id();
            subId = slashCommandMapping.sub();
            groupName = slashCommandMapping.group();
        } else if (mapping instanceof GenericCommandMapping discordMapping){
            clazz = discordMapping.event() == Event.class ? null : discordMapping.event();
            id = discordMapping.name();
            subId = discordMapping.subName();
            groupName = discordMapping.groupName();
            focusedOption = discordMapping.focussedOption();
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
        return true;
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
