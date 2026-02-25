package com.zgamelogic.discord.components.events;

import com.zgamelogic.discord.annotations.mappings.*;
import com.zgamelogic.discord.components.exceptions.DiscordExceptionEvent;
import com.zgamelogic.discord.services.ironwood.Model;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.zgamelogic.discord.helpers.Mapper.resolveParamsForArray;

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
        return switch (genericEvent) {
            case CommandAutoCompleteInteractionEvent event -> String.format(
                    "%s:%s:%s:%s:%s",
                    genericEvent.getClass().getSimpleName(),
                    event.getName(),
                    event.getSubcommandName() != null ? event.getSubcommandName() : "",
                    event.getSubcommandGroup() != null ? event.getSubcommandGroup() : "",
                    event.getFocusedOption().getName()
            );
            case GenericCommandInteractionEvent event -> String.format(
                    "%s:%s:%s:%s:%s",
                    genericEvent.getClass().getSimpleName(),
                    event.getName(),
                    event.getSubcommandName() != null ? event.getSubcommandName() : "",
                    event.getSubcommandGroup() != null ? event.getSubcommandGroup() : "",
                    ""
            );
            case ModalInteractionEvent event -> String.format(
                    "%s:%s:%s:%s:%s",
                    genericEvent.getClass().getSimpleName(),
                    event.getModalId(),
                    "",
                    "",
                    ""
            );
            case ButtonInteractionEvent event -> String.format(
                    "%s:%s:%s:%s:%s",
                    genericEvent.getClass().getSimpleName(),
                    event.getButton().getCustomId(),
                    "",
                    "",
                    ""
            );
            case GenericSelectMenuInteractionEvent<?, ?> event -> String.format(
                    "%s:%s:%s:%s:%s",
                    genericEvent.getClass().getSimpleName(),
                    event.getSelectMenu().getCustomId(),
                    "",
                    "",
                    ""
            );
            default -> String.format(
                    "%s:%s:%s:%s:%s",
                    genericEvent.getClass().getSimpleName(),
                    "",
                    "",
                    "",
                    ""
            );
        };
    }

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        return DiscordEvent.class.isAssignableFrom(eventType.toClass());
    }

    private Object[] resolveParamsForControllerMethod(Method method, GenericEvent event, Model model){
        return resolveParamsForArray(event, null, model, method.getParameters());
    }
}
