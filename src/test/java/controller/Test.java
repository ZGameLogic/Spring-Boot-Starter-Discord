package controller;

import com.zgamelogic.annotations.DiscordController;
import com.zgamelogic.annotations.DiscordMapping;
import com.zgamelogic.annotations.EventProperty;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.channel.forum.ForumTagAddEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.springframework.context.annotation.Bean;

import java.util.LinkedList;
import java.util.List;

@DiscordController
public class Test {
    @PostConstruct
    private void we(){
        System.out.println("Posted");
    }

//    @DiscordMapping(Id = "azure", SubId = "secret", FocusedOption = "name")
//    private void jjj(CommandAutoCompleteInteractionEvent event){
//        System.out.println(event.getFocusedOption().getValue());
//    }

    @DiscordMapping(Id = "azure", SubId = "secret")
    private void forumEvent(
            SlashCommandInteractionEvent event,
            @EventProperty User name
    ){
        System.out.println(name.getEffectiveName());
        event.replyModal(Modal.create(
                "secret_modal",
                "Ayo"
        ).addActionRow(
                TextInput.create("one", "1", TextInputStyle.SHORT).build()
        ).addActionRow(
                TextInput.create("two", "2", TextInputStyle.SHORT).build()).build()
        ).queue();
    }

    @DiscordMapping(Id = "secret_modal")
    private void modalEvent(
            @EventProperty String one,
            @EventProperty(name = "two") ModalMapping bep,
            @EventProperty ModalMapping three,
            ModalInteractionEvent event
    ){
        System.out.println(one);
        System.out.println(bep.getAsString());
        System.out.println(three);
    }


    @Bean
    private CommandData command(){
        return Commands.slash("testerooni", "ayo no");
    }
    @Bean
    private SlashCommandData commandTwo(){
        return Commands.slash("testerooyi", "ayo no");
    }

    @Bean
    private List<CommandData> commands() {
        return new LinkedList<>(List.of(
                Commands.slash("azure", "Commands dealing with Azure").addSubcommands(
                        new SubcommandData("secret", "Get a secret from the keyvault")
                                .addOption(OptionType.USER, "name", "Secret name to get the value of", true)
                )
        ));
    }
}
