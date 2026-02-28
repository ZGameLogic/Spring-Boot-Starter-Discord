package com.zgamelogic.discord.components.events;

import net.dv8tion.jda.api.events.GenericEvent;
import org.springframework.context.ApplicationEvent;

public class DiscordEvent extends ApplicationEvent {
    private final GenericEvent event;
    private final DiscordEventKey key;

    public DiscordEvent(Object source, GenericEvent event) {
        super(source);
        this.event = event;
        key = new DiscordEventKey(event);
    }

    public GenericEvent getEvent() {
        return event;
    }

    public DiscordEventKey getKey() {
        return key;
    }
}
