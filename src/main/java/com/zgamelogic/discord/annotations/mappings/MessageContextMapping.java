package com.zgamelogic.discord.annotations.mappings;

import com.zgamelogic.discord.components.events.DiscordEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@EventListener(value = DiscordEvent.class)
public @interface MessageContextMapping {
    @AliasFor("id")
    String value() default "";
    @AliasFor("value")
    String id() default "";
    String document() default "";
}
