package com.zgamelogic.discord.slash;

import lombok.Getter;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.context.ApplicationEvent;

public class SlashCommandInteractionEventApplicationEvent extends ApplicationEvent {
    @Getter
    private final SlashCommandInteractionEvent event;

    public SlashCommandInteractionEventApplicationEvent(Object source, SlashCommandInteractionEvent event) {
        super(source);
        this.event = event;
    }
}
