package com.zgamelogic.discord.components.events;

import com.zgamelogic.discord.annotations.DiscordExceptionHandler;
import com.zgamelogic.discord.annotations.mappings.*;
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
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.lang.annotation.Annotation;

public class DiscordEventKey {
    private final Class<? extends GenericEvent> clazz;
    private final String id;
    private final String subId;
    private final String groupName;
    private final String focusedOption;

    public DiscordEventKey(GenericEvent genericEvent){
        clazz = genericEvent.getClass();
        switch (genericEvent) {
            case CommandAutoCompleteInteractionEvent event -> {
                id = event.getName();
                subId = event.getSubcommandName() != null ? event.getSubcommandName() : null;
                groupName = event.getSubcommandGroup() != null ? event.getSubcommandGroup() : null;
                focusedOption = event.getFocusedOption().getName();
            }
            case GenericCommandInteractionEvent event -> {
                id = event.getName();
                subId = event.getSubcommandName() != null ? event.getSubcommandName() : null;
                groupName = event.getSubcommandGroup() != null ? event.getSubcommandGroup() : null;
                focusedOption = null;
            }
            case ModalInteractionEvent event -> {
                id = event.getModalId();
                subId = null;
                groupName = null;
                focusedOption = null;
            }
            case ButtonInteractionEvent event -> {
                id = event.getButton().getCustomId();
                subId = null;
                groupName = null;
                focusedOption = null;
            }
            case GenericSelectMenuInteractionEvent<?, ?> event -> {
                id = event.getSelectMenu().getCustomId();
                subId = null;
                groupName = null;
                focusedOption = null;
            }
            default -> {
                id = null;
                subId = null;
                groupName = null;
                focusedOption = null;
            }
        }
    }

    public DiscordEventKey(Annotation mapping, Method method) {
        switch (mapping) {
            case SlashCommandMapping slashCommandMapping -> {
                clazz = SlashCommandInteractionEvent.class;
                id = valueOrNull(AnnotatedElementUtils.findMergedAnnotation(method, SlashCommandMapping.class).id());
                subId = valueOrNull(slashCommandMapping.sub());
                groupName = valueOrNull(slashCommandMapping.group());
                focusedOption = null;
            }
            case SlashCommandAutocompleteMapping slashCommandMapping -> {
                clazz = CommandAutoCompleteInteractionEvent.class;
                id = valueOrNull(AnnotatedElementUtils.findMergedAnnotation(method, SlashCommandAutocompleteMapping.class).id());
                subId = valueOrNull(slashCommandMapping.sub());
                groupName = valueOrNull(slashCommandMapping.group());
                focusedOption = valueOrNull(slashCommandMapping.focused());
            }
            case MessageContextMapping _ -> {
                clazz = MessageContextInteractionEvent.class;
                id = valueOrNull(AnnotatedElementUtils.findMergedAnnotation(method, MessageContextMapping.class).id());
                subId = null;
                groupName = null;
                focusedOption = null;
            }
            case ButtonMapping _ -> {
                clazz = ButtonInteractionEvent.class;
                id = valueOrNull(AnnotatedElementUtils.findMergedAnnotation(method, ButtonMapping.class).id());
                subId = null;
                groupName = null;
                focusedOption = null;
            }
            case StringSelectMapping _ -> {
                clazz = StringSelectInteractionEvent.class;
                id = valueOrNull(AnnotatedElementUtils.findMergedAnnotation(method, StringSelectMapping.class).id());
                subId = null;
                groupName = null;
                focusedOption = null;
            }
            case EntitySelectMapping _ -> {
                clazz = EntitySelectInteractionEvent.class;
                id = valueOrNull(AnnotatedElementUtils.findMergedAnnotation(method, EntitySelectMapping.class).id());
                subId = null;
                groupName = null;
                focusedOption = null;
            }
            case ModalMapping _ -> {
                clazz = ModalInteractionEvent.class;
                id = valueOrNull(AnnotatedElementUtils.findMergedAnnotation(method, ModalMapping.class).id());
                subId = null;
                groupName = null;
                focusedOption = null;
            }
            case GenericDiscordMapping discordMapping -> {
                clazz = discordMapping.event();
                id = null;
                subId = null;
                groupName = null;
                focusedOption = null;
            }
            case DiscordExceptionHandler exceptionHandler -> {
                clazz = exceptionHandler.event();
                id = valueOrNull(exceptionHandler.id());
                subId = valueOrNull(exceptionHandler.sub());
                groupName = valueOrNull(exceptionHandler.group());
                focusedOption = valueOrNull(exceptionHandler.focused());
            }
            default ->
                    throw new IllegalArgumentException("Unsupported mapping type: " + mapping.annotationType().getName());
        }
    }

    private String valueOrNull(String value){
        return value.isEmpty() ? null : value;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof DiscordEventKey otherEvent)) return false;
        return
            (clazz == null || otherEvent.clazz.isAssignableFrom(clazz)) &&
            (id == null || id.equals(otherEvent.id)) &&
            (subId == null || subId.equals(otherEvent.subId)) &&
            (groupName == null || groupName.equals(otherEvent.groupName)) &&
            (focusedOption == null || focusedOption.equals(otherEvent.focusedOption));
    }
}
