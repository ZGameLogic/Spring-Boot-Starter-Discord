package com.zgamelogic.discord.components;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

@Component
public class DiscordHandlerAdapter {
    public void invoke(DiscordHandlerMapping.MethodHandle handle, GenericEvent event) throws Exception {
        Method method = handle.method();
        Object bean = handle.bean();

        Object[] args = resolveArguments(method, event);
        method.setAccessible(true);
        method.invoke(bean, args);
    }

    private Object[] resolveArguments(Method method, GenericEvent event) {
        // TODO map parameters
        return Arrays.stream(method.getParameters())
            .map(param -> {
                if (param.getType().equals(SlashCommandInteractionEvent.class)) {
                    return event;
                }
                // Add more resolver logic here
                return null;
            }).toArray();
    }
}
