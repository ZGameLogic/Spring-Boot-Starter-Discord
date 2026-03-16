package com.zgamelogic.discord.components.events;

import com.zgamelogic.discord.annotations.DiscordExceptionHandler;
import com.zgamelogic.discord.annotations.mappings.*;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class DiscordEventKey {
    private final Class<? extends GenericEvent> clazz;
    private final SpelExpressionParser parser;
    private final StandardEvaluationContext context;
    private final String spel;

    public DiscordEventKey(Annotation mapping, Method method) {
        parser = new SpelExpressionParser();
        context = new StandardEvaluationContext();

        String id;
        String subId;
        String groupName;
        String focusedOption;
        String spel = "";
        switch (mapping) {
            case SlashCommandMapping slashCommandMapping -> {
                clazz = SlashCommandInteractionEvent.class;
                spel = AnnotatedElementUtils.findMergedAnnotation(method, SlashCommandMapping.class).condition();
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
        if(spel.isBlank()) {
            List<String> expressions = new ArrayList<>();
            if (id != null) expressions.add("#root.event.getName() == '" + id + "'");
            if (subId != null) expressions.add("#root.event.getSubcommandName() == '" + subId + "'");
            if (groupName != null) expressions.add("#root.event.getGroupName() == '" + groupName + "'");
            if (focusedOption != null)
                expressions.add("#root.event.getFocusedOption().getName() == '" + focusedOption + "'");
            this.spel = expressions.isEmpty() ? "" : String.join(" && ", expressions);
        } else {
            this.spel = spel;
        }
    }

    private String valueOrNull(String value){
        return value.isEmpty() ? null : value;
    }

    public boolean matches(DiscordEvent event, Object bean){
        if (!clazz.isAssignableFrom(event.getEvent().getClass())) return false;
        context.setRootObject(event);
        // TODO maybe extract out other variables to context like event properties
        return parser.parseExpression(spel).getValue(context, Boolean.class);
    }
}
