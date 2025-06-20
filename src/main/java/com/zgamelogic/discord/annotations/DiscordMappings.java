package com.zgamelogic.discord.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DiscordMappings {
    /**
     * Array of DiscordMapping annotations to apply to the method.
     * This allows for multiple mappings to be defined for a single method.
     *
     * @see com.zgamelogic.discord.annotations.DiscordMapping
     * @return array of DiscordMapping annotations
     */
    DiscordMapping[] value();
}
