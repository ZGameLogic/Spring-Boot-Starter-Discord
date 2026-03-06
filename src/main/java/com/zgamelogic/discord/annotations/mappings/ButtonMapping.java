package com.zgamelogic.discord.annotations.mappings;

import com.zgamelogic.discord.components.events.DiscordEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for a mapping that catches button interactions.
 * @see net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
 * @author Ben Shabowski
 * @since 6.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@EventListener(value = DiscordEvent.class)
public @interface ButtonMapping {
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
     * The name of the document to respond to this event automatically.
     * If left blank, no document will be sent.
     * @return Document name to respond with
     */
    String document() default "";
}
