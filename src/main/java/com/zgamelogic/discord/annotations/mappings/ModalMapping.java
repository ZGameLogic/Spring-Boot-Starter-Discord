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
public @interface ModalMapping {
    @AliasFor("id")
    String value() default "";
    @AliasFor("value")
    String id() default "";
    /**
     * The name of the document to respond to this event automatically.
     * If left blank, no document will be sent.
     * @return Document name to respond with
     */
    String document() default "";
}
