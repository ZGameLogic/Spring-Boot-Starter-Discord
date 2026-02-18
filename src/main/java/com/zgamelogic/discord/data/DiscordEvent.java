package com.zgamelogic.discord.data;

import lombok.Getter;
import net.dv8tion.jda.api.events.GenericEvent;
import org.springframework.context.ApplicationEvent;

public class DiscordEvent extends ApplicationEvent {
    @Getter
    private final GenericEvent event;

    public DiscordEvent(Object source, GenericEvent event) {
        super(source);
        this.event = event;
    }
}
