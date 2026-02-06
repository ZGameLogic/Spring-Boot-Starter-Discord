package com.zgamelogic.discord.slash;

import com.zgamelogic.data.CobbleServiceException;
import com.zgamelogic.data.building.Building;
import com.zgamelogic.data.building.BuildingRepository;
import com.zgamelogic.data.enums.BuildingType;
import com.zgamelogic.data.npc.Npc;
import com.zgamelogic.data.player.Player;
import com.zgamelogic.discord.annotations.DiscordController;
import com.zgamelogic.discord.annotations.DiscordExceptionHandler;
import com.zgamelogic.discord.annotations.DiscordMapping;
import com.zgamelogic.discord.annotations.EventProperty;
import com.zgamelogic.services.CobbleDiscordHelperService;
import com.zgamelogic.services.CobbleService;
import com.zgamelogic.services.ResourceService;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.zgamelogic.data.Constants.*;

@DiscordController
@AllArgsConstructor
public class CobbleDiscord {
    private final ResourceService resourceService;
    private final CobbleDiscordHelperService helperService;
    private final CobbleService cobbleService;
    private final BuildingRepository buildingRepository;
    private final ApplicationEventPublisher ape;

    @DiscordMapping(Id = HELP_ID)
    private void cobbleHelp(SlashCommandInteractionEvent event) throws IOException {
        ape.publishEvent(new SlashCommandInteractionEventApplicationEvent(this, event));
        event
            .replyFiles(FileUpload.fromData(resourceService.getCobbleLogo().getInputStream(), "cobble-logo.png"))
            .addEmbeds(helperService.getHelpMessage(1))
            .addComponents(ActionRow.of(Button.secondary(HELP_PREV, "Previous page").asDisabled(), Button.secondary(HELP_NEXT, "Next Page")))
            .queue();
    }

    @DiscordMapping(Id = START_ID)
    private void cobbleStart(SlashCommandInteractionEvent event, @EventProperty(name = START_OPTION_TOWN) String townName) {
        try {
            if(townName == null || townName.isEmpty()) townName = event.getUser().getName() + "'s town";
            Player player = cobbleService.startCobblePlayer(event.getUser().getIdLong(), townName);
            event
                .replyFiles(FileUpload.fromData(resourceService.mapAppearanceAsStream(player.getNpcs().get(0).getAppearance()), "npc.png"))
                .addEmbeds(helperService.getStartMessage(player))
                .queue();
        } catch (CobbleServiceException | IOException e) {
            event.reply(e.getMessage()).setEphemeral(true).queue();
        }
    }

    @DiscordMapping(Id = CITIZENS_ID)
    private void cobbleCitizens(
        SlashCommandInteractionEvent event,
        @EventProperty String citizen
    ) throws CobbleServiceException, IOException {
        boolean citizenIncluded = citizen != null && !citizen.isEmpty();
        if (citizenIncluded) {
            helperService.cobbleCitizen(event, citizen);
        } else {
            helperService.cobbleCitizens(event);
        }
    }

    @DiscordMapping(Id = CITIZENS_ID, FocusedOption = CITIZENS_OPTION)
    private void cobbleCitizensAutocomplete(
        CommandAutoCompleteInteractionEvent event,
        @EventProperty String citizen
    ) throws CobbleServiceException {
        List<Npc> npcs = cobbleService.getCobbleNpcs(event.getUser().getIdLong());
        event.replyChoices(npcs.stream()
            .filter(npc -> citizen.isEmpty() || npc.getFirstName().toLowerCase().contains(citizen.toLowerCase()) ||  npc.getLastName().toLowerCase().contains(citizen.toLowerCase()))
            .map(npc -> new Command.Choice(npc.getFirstName() + " " + npc.getLastName(), npc.getId().toString()))
            .toList()
        ).queue();
    }

    @DiscordMapping(Id = BUILDING_ID, SubId = BUILDING_CODEX_ID, FocusedOption = BUILDING_OPTION)
    private void cobbleBuildingCodexAutocomplete(
        CommandAutoCompleteInteractionEvent event,
        @EventProperty String building
    ){
        event.replyChoices(Arrays.stream(BuildingType.values())
            .filter(type -> building.isEmpty() || type.getFriendlyName().toLowerCase().replaceAll("'", "").contains(building.toLowerCase()))
            .map(type -> new Command.Choice(type.getFriendlyName(), type.getFriendlyName()))
            .toList()
        ).queue();
    }

    @DiscordMapping(Id = BUILDING_ID, SubId = BUILDING_CODEX_ID)
    private void cobbleBuildingCodex(
        SlashCommandInteractionEvent event,
        @EventProperty String building
    ) throws CobbleServiceException {
        int page = 1;
        int maxPage = cobbleService.getCobbleBuildingList().size();
        if(building != null && BuildingType.validName(building)) {
            page = BuildingType.fromName(building).ordinal() + 1;
        }
        event.replyEmbeds(helperService.getBuildingMessage(page))
            .addComponents(ActionRow.of(
                Button.secondary(BUILDING_CODEX_PREV, "Previous page").withDisabled(page == 1),
                Button.secondary(BUILDING_CODEX_NEXT, "Next Page").withDisabled(page == maxPage)
            ))
            .queue();
    }

    @DiscordMapping(Id = BUILDING_ID, SubId = RENAME_ID, FocusedOption = RENAME_OPTION_NAME)
    private void cobbleRenameBuildingAutocomplete(
        CommandAutoCompleteInteractionEvent event,
        @EventProperty String building
    ){
        event.replyChoices(buildingRepository.findAllByPlayer_PlayerId(event.getUser().getIdLong()).stream()
            .filter(cb -> building == null || building.isEmpty() || cb.getBuildingName().toLowerCase().contains(building.toLowerCase()))
            .map(cb -> new Command.Choice(cb.getBuildingName(), cb.getBuildingId().toString()))
            .toList()
        ).queue();
    }

    @DiscordMapping(Id = BUILDING_ID, SubId = RENAME_ID)
    private void cobbleRenameBuildingSlashCommand(
        SlashCommandInteractionEvent event,
        @EventProperty String name,
        @EventProperty(name = RENAME_OPTION_NEW_NAME) String newName
    ) throws CobbleServiceException {
        Building building = cobbleService.getCobbleBuilding(event.getUser().getIdLong(), name);
        cobbleService.renameBuilding(building, newName);
        event.reply("Building successfully renamed to " + newName).setEphemeral(true).queue();
    }

    @DiscordMapping(Id = TOWN_ID, SubId = RENAME_ID)
    private void cobbleRenameTownSlashCommand(
        SlashCommandInteractionEvent event,
        @EventProperty(name = RENAME_OPTION_NEW_NAME) String name
    ) throws CobbleServiceException {
        Player player = cobbleService.getCobblePlayer(event.getUser().getIdLong());
        cobbleService.renameTown(player, name);
        event.reply("Town successfully renamed to " + name).setEphemeral(true).queue();
    }

    @DiscordMapping(Id = HELP_NEXT)
    @DiscordMapping(Id = HELP_PREV)
    private void cobbleHelpPageNext(ButtonInteractionEvent event){ helperService.cobbleHelpPage(event); }

    @DiscordMapping(Id = BUILDING_CODEX_NEXT)
    @DiscordMapping(Id = BUILDING_CODEX_PREV)
    private void cobbleBuildingCodexPageNext(ButtonInteractionEvent event){ helperService.cobbleBuildingCodexPage(event); }

    @DiscordExceptionHandler(CobbleServiceException.class)
    private void catchException(
        CobbleServiceException e,
        SlashCommandInteractionEvent slashEvent,
        CommandAutoCompleteInteractionEvent autoCompleteEvent
    ){
        if(slashEvent != null) {
            slashEvent.reply(e.getMessage()).setEphemeral(true).queue();
        } else if(autoCompleteEvent != null) {
            autoCompleteEvent.replyChoices(List.of()).queue();
        }
    }

    @Bean
    public List<CommandData> cobbleCommands(){
        return List.of(
            Commands.slash(HELP_ID, "Get some idea on how to play the game."),
            Commands.slash(START_ID, "Start the game of cobble!")
                .addOption(OptionType.STRING, START_OPTION_TOWN, "The name of your town", false),
            Commands.slash(PRODUCTION_ID, "Get an overview of production statistics for your town"),
            Commands.slash(CITIZENS_ID, "Get information on citizens in your town")
                .addOption(OptionType.STRING, CITIZENS_OPTION, "Get a specific citizen", false, true),
            Commands.slash(BUILDING_ID, "building stuff").addSubcommands(
                new SubcommandData(BUILDING_CODEX_ID, "Get a book of buildings and what they do")
                    .addOption(OptionType.STRING, BUILDING_OPTION, "Start the book on a specific building", false, true),
                new SubcommandData(RENAME_ID, "Rename a building")
                    .addOption(OptionType.STRING, RENAME_OPTION_NAME, "The current name of the building", true, true)
                    .addOption(OptionType.STRING, RENAME_OPTION_NEW_NAME, "The new name of the building", true)
            ),
            Commands.slash(TOWN_ID, "town stuff").addSubcommands(
                new SubcommandData(RENAME_ID, "Rename your town")
                    .addOption(OptionType.STRING, RENAME_OPTION_NEW_NAME, "The new name of the town", true)
            ),
            Commands.slash(SCHEDULE_ID, "Schedule things to happen").addSubcommands(
                new SubcommandData(SCHEDULE_BUILD_ID, "Schedule a building to be built during the day")
                    .addOption(OptionType.STRING, BUILDING_OPTION, "The building to be scheduled to be built", true, true)
                    .addOption(OptionType.STRING, NPC_OPTION, "The NPC to assign to this building when built", true, true),
                new SubcommandData(SCHEDULE_UPGRADE_ID, "Schedule a building to be upgraded during the day")
                    .addOption(OptionType.STRING, BUILDING_OPTION, "The building to be scheduled to be upgraded", true, true)
                    .addOption(OptionType.STRING, NPC_OPTION, "The NPC to assign to this building after upgrade if required", false, true)
            )
        );
    }
}
