package com.zgamelogic.discord.components;

import com.zgamelogic.discord.slash.DiscordEvent;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Discord Listener is the integration point between Spring and JDA. This listener is from JDA and forwards all events to the dispatcher.
 */
@Component
public class DiscordListener extends ListenerAdapter {
    private final ApplicationEventPublisher eventPublisher;

    public DiscordListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onGenericEvent(@NotNull GenericEvent event) {
        eventPublisher.publishEvent(new DiscordEvent(this, event));
    }
}
