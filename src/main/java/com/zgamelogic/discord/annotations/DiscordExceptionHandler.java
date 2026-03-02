package com.zgamelogic.discord.annotations;

import com.zgamelogic.discord.components.events.DiscordEvent;
import net.dv8tion.jda.api.events.Event;
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
    Class<? extends Event> event() default Event.class;
    String id() default "";
    String group() default "";
    String sub() default "";
    String focused() default "";
    String document() default "";
}
