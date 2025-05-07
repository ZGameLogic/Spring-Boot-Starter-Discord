package com.zgamelogic.discord.components;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

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
