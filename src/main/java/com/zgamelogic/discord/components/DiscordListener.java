package com.zgamelogic.discord.components;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * Discord Listener is the integration point between Spring and JDA. This listener is from JDA and forwards all events to the dispatcher.
 */
@Component
public class DiscordListener extends ListenerAdapter {
    private final DiscordDispatcher dispatcher;

    public DiscordListener(DiscordDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void onGenericEvent(@NotNull GenericEvent event) {
        dispatcher.dispatch(event);
    }
}
