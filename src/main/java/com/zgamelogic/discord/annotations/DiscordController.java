package com.zgamelogic.discord.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Annotation to denote a class to be a controller for discord events.
 * @author Ben Shabowski
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface DiscordController {}
