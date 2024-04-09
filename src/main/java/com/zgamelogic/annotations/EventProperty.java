package com.zgamelogic.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for method parameters to auto-map SlashCommandInteractionEvent options, AutoCompleteCommandInteractionEvent options, and ModelInteractionEvent options to method parameters when called.
 * If the command option is null, or the command does not contain that option, then the annotated method parameter will be null.
 * This can also be applied to a custom java object to map the data to an object.
 * @see net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
 * @see net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
 * @see net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
 * @author Ben Shabowski
 * @since 1.2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface EventProperty {
    /**
     * Use this to look for a specific name of the command option value.
     * If omitted, it will use the method parameter name
     * @return name of command option value
     */
    String name() default "";
}
