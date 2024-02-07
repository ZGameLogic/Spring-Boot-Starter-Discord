package com.zgamelogic.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to denote auto called methods for discord events.
 * These methods must include an event parameter
 * @author Ben Shabowski
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DiscordMapping {
    // slash command options
    String SlashCommandId() default "";
    String SlashCommandSubId() default "";
    String SlashCommandFocusedOption() default "";
}
