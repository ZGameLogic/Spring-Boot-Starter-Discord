package com.zgamelogic.discord.annotations;

import com.zgamelogic.discord.components.events.DiscordEvent;
import org.springframework.context.event.EventListener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exception handler annotation
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@EventListener(value = DiscordEvent.class)
public @interface DiscordExceptionHandler {
    Class<? extends Throwable>[] value();
}
