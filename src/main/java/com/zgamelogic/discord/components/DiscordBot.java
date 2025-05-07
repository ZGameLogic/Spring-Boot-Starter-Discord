package com.zgamelogic.discord.components;

import com.zgamelogic.discord.helpers.Translator;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.slf4j.Logger;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedList;
import java.util.List;

/**
 * Autoconfiguration class for the Discord bot.
 * This will also register all methods for the custom discord listener so that they get called on that specific event
 *
 * @author Ben Shabowski
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(DiscordBotProperties.class)
public class DiscordBot {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DiscordBot.class);

    private final JDA bot;
    /**
     * Creates a configuration with specific properties and application contexts
     *
     * @param properties Properties to create the bot builder with
     * @param beans      Listable Bean factory that holds the beans of command data to be auto-injected into the bot
     * @author Ben Shabowski
     * @since 1.0.0
     */
    public DiscordBot(
        DiscordBotProperties properties,
        ListableBeanFactory beans,
        DiscordListener listener,
        @Autowired(required = false) JDABuilder beanBuilder
    ){
        log.debug("Bean JDABuilder present {}", beanBuilder != null);
        JDABuilder builder = beanBuilder == null ? JDABuilder.createDefault(properties.getToken()) : beanBuilder;
        if(beanBuilder == null) {
            if(properties.getGatewayIntents() != null) {
                for(String intent : properties.getGatewayIntents()) {
                    Translator.stringToIntent(intent).ifPresentOrElse(i -> {
                        log.debug("Enabled intent: {}", i.name());
                        builder.enableIntents(i);
                    }, () -> log.warn("Unable to decode {} gateway intent", intent));
                }
            }
            if(properties.getCacheFlags() != null) {
                for(String cacheFlag : properties.getCacheFlags()) {
                    Translator.stringToCache(cacheFlag).ifPresentOrElse(e -> {
                        log.debug("Enabled cache: {}", e.name());
                        builder.enableCache(e);
                    }, () -> log.warn("Unable to decode {} cache flag", cacheFlag));
                }
            }
            if(properties.getMemberCachePolicy() != null) {
                Translator.stringToMemberCachePolicy(properties.getMemberCachePolicy()).ifPresentOrElse(mcp -> {
                    log.debug("Enabled member cache policy: {}", mcp);
                    builder.setMemberCachePolicy(mcp);
                }, () -> log.warn("Unable to decode {} member cache policy", properties.getMemberCachePolicy()));
            }
            builder.setEventPassthrough(properties.isEventPassthrough());
        } else {
            log.debug("Skipping configuration from properties files since spring found a bean for JDABuilder.");
        }
        builder.addEventListeners(listener);

        try {
            bot = builder.build().awaitReady();
            List<CommandData> commandData = new LinkedList<>(beans.getBeansOfType(CommandData.class).values());
            beans.getBeansOfType(List.class).values().forEach(list -> {
                if(!list.isEmpty() && list.get(0) instanceof CommandData){
                    list.stream().filter(item -> item instanceof CommandData).forEach(command -> commandData.add((CommandData) command));
                }
            });
            if(!commandData.isEmpty()) {
                bot.updateCommands().addCommands(commandData).queue();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public JDA bot(){
        return bot;
    }
}
