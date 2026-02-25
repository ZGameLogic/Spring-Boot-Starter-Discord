package com.zgamelogic.discord.components;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.slf4j.Logger;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@Component
@EnableConfigurationProperties(DiscordBotProperties.class)
public class DiscordBot implements SmartInitializingSingleton {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DiscordBot.class);

    private final DiscordBotProperties properties;
    private final ListableBeanFactory beans;
    private final DiscordListener listener;
    private final JDABuilder beanBuilder;

    public DiscordBot(
            DiscordBotProperties properties,
            ListableBeanFactory beans,
            DiscordListener listener,
            @Autowired(required = false) JDABuilder beanBuilder
    ){
        this.properties = properties;
        this.beans = beans;
        this.listener = listener;
        this.beanBuilder = beanBuilder;
    }

    @Override
    public void afterSingletonsInstantiated() {
        log.debug("Bean JDABuilder present {}", beanBuilder != null);
        JDABuilder builder = beanBuilder == null ? JDABuilder.createDefault(properties.getToken()) : beanBuilder;
        if(beanBuilder == null) {
            if(properties.getGatewayIntents() != null)
                builder.enableIntents(Arrays.asList(properties.getGatewayIntents()));
            if(properties.getCacheFlags() != null)
                builder.enableCache(Arrays.asList(properties.getCacheFlags()));
            if(properties.getMemberCachePolicy() != null)
                builder.setMemberCachePolicy(properties.getMemberCachePolicy());
            builder.setEventPassthrough(properties.isEventPassthrough());
        } else {
            log.debug("Skipping configuration from properties files since spring found a bean for JDABuilder.");
        }
        builder.addEventListeners(listener);

        try {
            JDA bot = builder.build().awaitReady();
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
}
