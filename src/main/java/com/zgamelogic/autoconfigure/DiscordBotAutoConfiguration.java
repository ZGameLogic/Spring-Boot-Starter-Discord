package com.zgamelogic.autoconfigure;

import com.zgamelogic.annotations.Bot;
import com.zgamelogic.annotations.DiscordController;
import com.zgamelogic.annotations.DiscordMapping;
import com.zgamelogic.helpers.Translator;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.slf4j.Logger;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 *
 * @author Ben Shabowski
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(DiscordBotProperties.class)
public class DiscordBotAutoConfiguration {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DiscordBotAutoConfiguration.class);

    /**
     * Creates a configuration with specific properties and application contexts
     *
     * @param properties Properties to create the bot builder with
     * @param context    Application context that holds all the discord controllers
     * @param beans      Listable Bean factory that holds the beans of command data to be auto-injected into the bot
     * @author Ben Shabowski
     * @since 1.0.0
     */
    public DiscordBotAutoConfiguration(
            DiscordBotProperties properties,
            ApplicationContext context,
            ListableBeanFactory beans,
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
        DiscordListener listener = new DiscordListener();
        context.getBeansWithAnnotation(DiscordController.class).forEach((controllerClassName, controllerObject) -> {
            for(Method method: controllerObject.getClass().getDeclaredMethods()){
                if(!method.isAnnotationPresent(DiscordMapping.class)) continue;
                // TODO check if the method has an object and that that object has a default no args constructor
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
