package com.zgamelogic.controllers;

import com.zgamelogic.annotations.DiscordController;
import com.zgamelogic.annotations.DiscordMapping;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;

@DiscordController
@Slf4j
public class DiscordTestController {

    @Autowired
    public DiscordTestController(){
        log.info("Test controller autowired");
    }

    @DiscordMapping
    public void test(MessageReceivedEvent event){
        System.out.println(event.getMessage().getContentRaw());
    }

    @DiscordMapping
    public void test2(SlashCommandInteractionEvent event){
        System.out.println();
        System.out.println(event.getSubcommandName());
    }
}
