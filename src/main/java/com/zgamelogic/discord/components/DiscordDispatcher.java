package com.zgamelogic.discord.components;

import net.dv8tion.jda.api.events.GenericEvent;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class DiscordDispatcher {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DiscordDispatcher.class);

    private final DiscordHandlerMapping handlerMapping;
    private final DiscordHandlerAdapter handlerAdapter;

    public DiscordDispatcher(DiscordHandlerMapping handlerMapping, DiscordHandlerAdapter handlerAdapter) {
        this.handlerMapping = handlerMapping;
        this.handlerAdapter = handlerAdapter;
    }

    public void dispatch(GenericEvent event) {
        try {
            var handler = handlerMapping.findHandlerFor(event);
            if (handler != null) {
                handlerAdapter.invoke(handler, event);
            }
        } catch (Exception ex) {
            log.error("Error dispatching event", ex);
        }
    }
}
