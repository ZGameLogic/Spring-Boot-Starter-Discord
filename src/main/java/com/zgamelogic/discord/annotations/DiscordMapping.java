package com.zgamelogic.discord.annotations;

import net.dv8tion.jda.api.events.Event;

import java.lang.annotation.*;

/**
 * Annotation to denote auto called methods for discord events.
 * These methods must include an event parameter
 * @author Ben Shabowski
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(DiscordMappings.class)
public @interface DiscordMapping {

    /**
     * Use this to have this method only called when the id of a command/interaction equals this string.
     * This works with the following
     * @see net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
     * @see net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
     * @see net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
     * @see net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
     * @see net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
     * @see net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
     * @see net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
     * @return id/name of the interaction you would like to match it with
     */
    String Id() default "";

    /**
     * Use this to have this method only called when the sub command id of a command equals this string.
     * This works with the following
     * @see net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
     * @see net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
     * @return sub-id of the command you would like to match it with
     */
    String SubId() default "";

    /**
     * Use this to have this method only called when the group name of a command equals this string.
     * This works with the following
     * @see net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
     * @see net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
     * @return group name of the command you would like to match it with
     */
    String GroupName() default "";

    /**
     * Use this to have this method only called when the focused option of an interaction event.
     * @see net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
     * @see net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
     * @return focused option of the command you would like to match it with
     */
    String FocusedOption() default "";

    /**
     * Use this to specify the event class without declaring it as the first method parameter
     * @return Event class that the event listens to
     */
    Class<? extends Event> Event() default Event.class;

    /**
     * Use this to specify the document name to respond to a message
     * @return Document name
     */
    String Document() default "";
}
