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
    private final String spel;

    public DiscordEventKey(Annotation mapping, Method method) {
        parser = new SpelExpressionParser();

        String spel;
        List<String> expressions = new ArrayList<>();
        switch (mapping) {
            case SlashCommandMapping slashCommandMapping -> {
                clazz = SlashCommandInteractionEvent.class;
                spel = slashCommandMapping.condition();
                addExpression("#root.getName() == '%s'", AnnotatedElementUtils.findMergedAnnotation(method, SlashCommandMapping.class).id(), expressions);
                addExpression("#root.getSubcommandName() == '%s'", slashCommandMapping.sub(), expressions);
                addExpression("#root.getSubcommandGroup() == '%s'", slashCommandMapping.group(), expressions);
            }
            case SlashCommandAutocompleteMapping slashCommandMapping -> {
                clazz = CommandAutoCompleteInteractionEvent.class;
                spel = slashCommandMapping.condition();
                addExpression("#root.getName() == '%s'", AnnotatedElementUtils.findMergedAnnotation(method, SlashCommandAutocompleteMapping.class).id(), expressions);
                addExpression("#root.getSubcommandName() == '%s'", slashCommandMapping.sub(), expressions);
                addExpression("#root.getSubcommandGroup() == '%s'", slashCommandMapping.group(), expressions);
                addExpression("#root.getFocusedOption().getName() == '%s'", slashCommandMapping.focused(), expressions);
            }
            case MessageContextMapping messageContextMapping -> {
                clazz = MessageContextInteractionEvent.class;
                spel = messageContextMapping.condition();
                addExpression("#root.getName() == '%s'", AnnotatedElementUtils.findMergedAnnotation(method, MessageContextMapping.class).id(), expressions);
            }
            case ButtonMapping buttonMapping -> {
                clazz = ButtonInteractionEvent.class;
                spel = buttonMapping.condition();
                addExpression("#root.getButton().getCustomId() == '%s'", AnnotatedElementUtils.findMergedAnnotation(method, ButtonMapping.class).id(), expressions);
            }
            case StringSelectMapping stringSelectMapping -> {
                clazz = StringSelectInteractionEvent.class;
                spel = stringSelectMapping.condition();
                addExpression("#root.getCustomId() == '%s'", AnnotatedElementUtils.findMergedAnnotation(method, StringSelectMapping.class).id(), expressions);
            }
            case EntitySelectMapping entitySelectMapping -> {
                clazz = EntitySelectInteractionEvent.class;
                spel = entitySelectMapping.condition();
                addExpression("#root.getCustomId() == '%s'", AnnotatedElementUtils.findMergedAnnotation(method, EntitySelectMapping.class).id(), expressions);
            }
            case ModalMapping modalMapping -> {
                clazz = ModalInteractionEvent.class;
                spel = modalMapping.condition();
                addExpression("#root.getCustomId() == '%s'", AnnotatedElementUtils.findMergedAnnotation(method, ModalMapping.class).id(), expressions);
            }
            case GenericDiscordMapping discordMapping -> {
                clazz = discordMapping.event();
                spel = discordMapping.condition();
            }
            case DiscordExceptionHandler exceptionHandler -> {
                clazz = exceptionHandler.event();
                spel = exceptionHandler.condition();
            }
            default ->
                    throw new IllegalArgumentException("Unsupported mapping type: " + mapping.annotationType().getName());
        }
        if(spel.isBlank()) {
            this.spel = expressions.isEmpty() ? "" : String.join(" && ", expressions);
        } else {
            this.spel = spel;
        }
    }

    private String valueOrNull(String value){
        return value.isEmpty() ? null : value;
    }

    private void addExpression(String expression, String value, List<String> expressions) {
        if (value != null && !value.isBlank()) expressions.add(String.format(expression, value));
    }

    public boolean matches(GenericEvent event, Method method, Object[] params){
        if (!clazz.isAssignableFrom(event.getClass())) return false;
        if(spel.isBlank()) return true;
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setRootObject(event);
        for(int i = 0; i < params.length; i++){
            context.setVariable(method.getParameters()[i].getName(), params[i]);
        }
        return parser.parseExpression(spel).getValue(context, Boolean.class);
    }
}
