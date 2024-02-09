package com.zgamelogic.controllers;

import com.zgamelogic.annotations.Bot;
import com.zgamelogic.annotations.DiscordController;
import com.zgamelogic.annotations.DiscordMapping;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.springframework.beans.factory.annotation.Autowired;

@DiscordController
@Slf4j
public class DiscordTestController {
    @Bot
    private JDA bot;

    @Autowired
    public DiscordTestController(){
        log.info("Test controller autowired");
    }

//    @DiscordMapping
//    public void test(MessageReceivedEvent event){
//        System.out.println(event.getMessage().getContentRaw());
//    }

    @DiscordMapping(Id = "send_text")
    public void test2(SlashCommandInteractionEvent event){
        event.reply("uh").addActionRow(Button.danger("test_button", "Ayo")).queue();
    }

    @DiscordMapping(Id = "test_button")
    private void test3(ButtonInteractionEvent event){
        event.reply("YOU PUSHED MY BUTTON").queue();
    }

    @DiscordMapping(Id = "Read")
    private void test4(MessageContextInteractionEvent event){
        event.reply(event.getInteraction().getTarget().getContentStripped()).queue();
    }
}
