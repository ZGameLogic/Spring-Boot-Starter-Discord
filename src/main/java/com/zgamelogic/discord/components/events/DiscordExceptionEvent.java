package com.zgamelogic.discord.components.events;

import net.dv8tion.jda.api.events.GenericEvent;
import org.springframework.context.ApplicationEvent;

public class DiscordExceptionEvent extends ApplicationEvent {
    private final GenericEvent event;
    private final Throwable exception;

    public DiscordExceptionEvent(Object source, GenericEvent event, Throwable exception) {
        super(source);
        this.event = event;
        this.exception = exception;
    }

    public GenericEvent getEvent() {
        return event;
    }

    public Throwable getException() {
        return exception;
    }
}
