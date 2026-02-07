package com.zgamelogic.discord.slash;

import org.springframework.context.event.EventListener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@EventListener(value = SlashCommandInteractionEventApplicationEvent.class)
public @interface SlashCommandMapping {
    String id();
    String group() default "";
    String sub() default "";
}
