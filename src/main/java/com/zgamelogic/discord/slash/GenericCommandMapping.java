package com.zgamelogic.discord.slash;

import net.dv8tion.jda.api.events.Event;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@EventListener(value = DiscordEvent.class)
public @interface GenericCommandMapping {
    @AliasFor("name")
    String value() default "";
    @AliasFor("value")
    String name() default "";
    String groupName() default "";
    String subName() default "";
    String focussedOption() default "";
    Class<? extends Event> event() default Event.class;
}
