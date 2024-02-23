package com.zgamelogic.autoconfigure;

import com.zgamelogic.annotations.Bot;
import com.zgamelogic.annotations.DiscordController;
import com.zgamelogic.annotations.DiscordMapping;
import com.zgamelogic.helpers.Translator;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * Autoconfiguration class for the Discord bot.
 * This will also register all methods for the custom discord listener so that they get called on that specific event
 * @author Ben Shabowski
 * @since 1.0.0
 */
@Configuration
@Slf4j
@EnableConfigurationProperties(DiscordBotProperties.class)
public class DiscordBotAutoConfiguration {

    /**
     * Creates a configuration with specific properties and application contexts
     * @param properties Properties to create the bot builder with
     * @param context Application context that holds all the discord controllers
     * @param beans Listable Bean factory that holds the beans of command data to be auto-injected into the bot
     * @author Ben Shabowski
     * @since 1.0.0
     */
    public DiscordBotAutoConfiguration(DiscordBotProperties properties, ApplicationContext context, ListableBeanFactory beans){
        JDABuilder builder = JDABuilder.createDefault(properties.getToken());
        if(properties.getGatewayIntents() != null) {
            for (String intent : properties.getGatewayIntents()) {
                Translator.stringToIntent(intent).ifPresentOrElse(builder::enableIntents,
                        () -> log.warn("Unable to decode {} gateway intent", intent));
            }
        }
        if(properties.getCacheFlags() != null) {
            for (String cacheFlag : properties.getCacheFlags()) {
                Translator.stringToCache(cacheFlag).ifPresentOrElse(builder::enableCache,
                        () -> log.warn("Unable to decode {} cache flag", cacheFlag));
            }
        }
        if(properties.getMemberCachePolicy() != null) {
            Translator.stringToMemberCachePolicy(properties.getMemberCachePolicy()).ifPresentOrElse(builder::setMemberCachePolicy,
                    () -> log.warn("Unable to decode {} member cache policy", properties.getMemberCachePolicy()));
        }
        builder.setEventPassthrough(properties.isEventPassthrough());
        DiscordListener listener = new DiscordListener();
        context.getBeansWithAnnotation(DiscordController.class).forEach((controllerClassName, controllerObject) -> {
            for(Method method: controllerObject.getClass().getDeclaredMethods()){
                if(!method.isAnnotationPresent(DiscordMapping.class)) continue;
                listener.addObjectMethod(controllerObject, method);
            }
            for(Field field: controllerObject.getClass().getDeclaredFields()){
                if(field.isAnnotationPresent(Bot.class)){
                    if(field.getType() != JDA.class) {
                        log.error("@Bot fields must be of type JDA");
                        throw new RuntimeException("@Bot fields must be of type JDA");
                    }
                    listener.addReadyObjectField(controllerObject, field);
                }
            }
        });

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
