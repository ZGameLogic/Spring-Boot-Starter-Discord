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
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
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

    @DiscordMapping
    public void test(MessageReceivedEvent event){
        System.out.println(event.getMessage().getContentRaw());
    }

    @DiscordMapping(Id = "send_text")
    public void test2(SlashCommandInteractionEvent event){
        TextInput message = TextInput.create("message", "Reply", TextInputStyle.PARAGRAPH).build();
        event.replyModal(Modal.create("reply_text_modal", "Message response").addActionRow(message).build()).queue();
    }

    @DiscordMapping(Id = "plan_event")
    public void test7(SlashCommandInteractionEvent event){
        event.reply("no").queue();
    }

    @DiscordMapping(Id = "ayo", FocusedOption = "ope")
    public void test4(CommandAutoCompleteInteractionEvent event){
        System.out.println(event.getName());
        System.out.println(event.getFocusedOption().getName());
        System.out.println(event.getSubcommandName());
        event.replyChoiceStrings("one", "two", "three").queue();
    }

    @DiscordMapping
    public void test3(ModalInteractionEvent event){
        System.out.println(event.getModalId());
    }

    @DiscordMapping
    private void onLogin(ReadyEvent event){
//        bot.getGuildById(817802830622359552L).upsertCommand(
//                Commands.slash("ayo", "I dont think so")
//                        .addOption(OptionType.STRING, "ope", "Nah nah nah", true, true)
//        ).queue();
    }

    @DiscordMapping
    private void jjjjj(UserContextInteractionEvent event){}

    @DiscordMapping
    private void kkkkk(MessageContextInteractionEvent event){
        event.reply("Oh many cool").queue();
    }
}
