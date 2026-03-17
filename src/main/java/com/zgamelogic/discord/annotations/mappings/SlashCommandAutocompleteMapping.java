package com.zgamelogic.discord.annotations.mappings;

import com.zgamelogic.discord.components.events.DiscordEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Ben Shabowski
 * @since 6.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@EventListener(value = DiscordEvent.class)
public @interface SlashCommandAutocompleteMapping {
    /**
     * Alias for id
     * @return The custom id given to an interaction to identify the event to catch
     */
    @AliasFor("id")
    String value() default "";
    /**
     * The id of the event to catch. This is the custom id/name given to an interaction when responding to an event.
     * If value and id are left blank, this mapping will catch all interactions.
     * @return The custom id given to an interaction to identify the event to catch
     */
    @AliasFor("value")
    String id() default "";
    /**
     * The group of the interaction to catch. This is the custom group given to an interaction when responding to an event.
     * If group is left blank, this mapping will catch all interactions with any group.
     * @return The custom group given to an interaction to identify the event to catch
     */
    String group() default "";
    /**
     * The sub id of the interaction to catch. This is the custom sub id given to an interaction when responding to an event.
     * If the sub id is left blank, this mapping will catch all interactions with any sub id.
     * @return The custom sub id given to an interaction to identify the event to catch
     */
    String sub() default "";

    /**
     * The name of the focused option to catch. This is the name of the focused option given to an interaction when responding to an event.
     * If focused is left blank, this mapping will catch all interaction with any focused option.
     * @return The custom name of the focused option given to an interaction ot identify the event to catch
     */
    String focused() default "";

    /**
     * Spring Expression Language (SpEL) expression that overrides the default matching behavior
     * @return Spring Expression Language (SpEL) expression that overrides the default matching behavior
     */
    @AliasFor(annotation = EventListener.class, attribute = "condition")
    String condition() default "";
}
