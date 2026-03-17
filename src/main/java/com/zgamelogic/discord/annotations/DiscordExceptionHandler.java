package com.zgamelogic.discord.annotations;

import com.zgamelogic.discord.components.events.DiscordEvent;
import net.dv8tion.jda.api.events.Event;
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
public @interface DiscordExceptionHandler {
    Class<? extends Throwable>[] value();
    Class<? extends Event> event() default Event.class;
    String id() default "";
    String group() default "";
    String sub() default "";
    String focused() default "";
    /**
     * The name of the document to respond to this event automatically.
     * If left blank, no document will be sent.
     * @return Document name to respond with
     */
    String document() default "";

    /**
     * Spring Expression Language (SpEL) expression that overrides the default matching behavior
     * @return Spring Expression Language (SpEL) expression that overrides the default matching behavior
     */
    @AliasFor(annotation = EventListener.class, attribute = "condition")
    String condition() default "";
}
