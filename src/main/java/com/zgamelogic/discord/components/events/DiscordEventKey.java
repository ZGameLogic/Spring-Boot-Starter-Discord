package com.zgamelogic.discord.components.events;

import com.zgamelogic.discord.annotations.mappings.*;
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
                subId = event.getSubcommandName() != null ? event.getSubcommandName() : "";
                groupName = event.getSubcommandGroup() != null ? event.getSubcommandGroup() : "";
                focusedOption = event.getFocusedOption().getName();
            }
            case GenericCommandInteractionEvent event -> {
                id = event.getName();
                subId = event.getSubcommandName() != null ? event.getSubcommandName() : "";
                groupName = event.getSubcommandGroup() != null ? event.getSubcommandGroup() : "";
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
                id = AnnotatedElementUtils.findMergedAnnotation(method, SlashCommandMapping.class).id();
                subId = slashCommandMapping.sub();
                groupName = slashCommandMapping.group();
                focusedOption = null;
            }
            case SlashCommandAutocompleteMapping slashCommandMapping -> {
                clazz = CommandAutoCompleteInteractionEvent.class;
                id = AnnotatedElementUtils.findMergedAnnotation(method, SlashCommandAutocompleteMapping.class).id();
                subId = slashCommandMapping.sub();
                groupName = slashCommandMapping.group();
                focusedOption = slashCommandMapping.focused();
            }
            case MessageContextMapping messageContextMapping -> {
                clazz = MessageContextInteractionEvent.class;
                id = AnnotatedElementUtils.findMergedAnnotation(method, MessageContextMapping.class).id();
                subId = null;
                groupName = null;
                focusedOption = null;
            }
            case ButtonMapping buttonMapping -> {
                clazz = ButtonInteractionEvent.class;
                id = AnnotatedElementUtils.findMergedAnnotation(method, ButtonMapping.class).id();
                subId = null;
                groupName = null;
                focusedOption = null;
            }
            case StringSelectMapping stringSelectMapping -> {
                clazz = StringSelectInteractionEvent.class;
                id = AnnotatedElementUtils.findMergedAnnotation(method, StringSelectMapping.class).id();
                subId = null;
                groupName = null;
                focusedOption = null;
            }
            case EntitySelectMapping entitySelectMapping -> {
                clazz = EntitySelectInteractionEvent.class;
                id = AnnotatedElementUtils.findMergedAnnotation(method, EntitySelectMapping.class).id();
                subId = null;
                groupName = null;
                focusedOption = null;
            }
            case ModalMapping modalMapping -> {
                clazz = ModalInteractionEvent.class;
                id = AnnotatedElementUtils.findMergedAnnotation(method, ModalMapping.class).id();
                subId = null;
                groupName = null;
                focusedOption = null;
            }
            case GenericDiscordMapping discordMapping -> {
                clazz = discordMapping.event() == Event.class ? null : discordMapping.event();
                id = null;
                subId = null;
                groupName = null;
                focusedOption = null;
            }
            default ->
                    throw new IllegalArgumentException("Unsupported mapping type: " + mapping.annotationType().getName());
        }
    }


    @Override
    public boolean equals(Object obj) {
        if(obj instanceof DiscordEventKey otherEvent){

        }

        return false;
    }
}
