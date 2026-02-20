package com.zgamelogic.discord.data;

import net.dv8tion.jda.api.events.GenericEvent;
import org.springframework.context.ApplicationEvent;

public class DiscordExceptionEvent extends ApplicationEvent {
    private final GenericEvent event;

    public DiscordExceptionEvent(Object source, GenericEvent event){
        super(source);
        this.event = event;
    }

    public GenericEvent getEvent() {
        return event;
    }
}
