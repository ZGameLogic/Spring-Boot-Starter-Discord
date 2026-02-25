package com.zgamelogic.discord.components.events;

import net.dv8tion.jda.api.events.GenericEvent;
import org.springframework.context.ApplicationEvent;

public class DiscordEvent extends ApplicationEvent {
    private final GenericEvent event;

    public DiscordEvent(Object source, GenericEvent event) {
        super(source);
        this.event = event;
    }

    public GenericEvent getEvent() {
        return event;
    }
}
