package com.zgamelogic.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DiscordMapping {
    // slash command options
    String SlashCommandId() default "";
    String SlashCommandSubId() default "";
    String SlashCommandFocusedOption() default "";
}
