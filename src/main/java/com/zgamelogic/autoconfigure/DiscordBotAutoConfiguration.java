package com.zgamelogic.autoconfigure;

import com.zgamelogic.annotations.DiscordController;
import com.zgamelogic.annotations.DiscordMapping;
import com.zgamelogic.helpers.PropsToBuilder;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDABuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

@Configuration
@Slf4j
@EnableConfigurationProperties(DiscordBotProperties.class)
public class DiscordBotAutoConfiguration {
    public DiscordBotAutoConfiguration(DiscordBotProperties properties, ApplicationContext context){
        JDABuilder builder = JDABuilder.createDefault(properties.getToken());
        if(properties.getGatewayIntents() != null) {
            for (String intent : properties.getGatewayIntents()) {
                PropsToBuilder.stringToIntent(intent).ifPresentOrElse(builder::enableIntents,
                        () -> log.warn("Unable to decode {} gateway intent", intent));
            }
        }
        if(properties.getCacheFlags() != null) {
            for (String cacheFlag : properties.getCacheFlags()) {
                PropsToBuilder.stringToCache(cacheFlag).ifPresentOrElse(builder::enableCache,
                        () -> log.warn("Unable to decode {} cache flag", cacheFlag));
            }
        }
        if(properties.getMemberCachePolicy() != null) {
            PropsToBuilder.stringToMemberCachePolicy(properties.getMemberCachePolicy()).ifPresentOrElse(builder::setMemberCachePolicy,
                    () -> log.warn("Unable to decode {} member cache policy", properties.getMemberCachePolicy()));
        }
        builder.setEventPassthrough(properties.isEventPassthrough());

        DiscordListener listener = new DiscordListener();
        context.getBeansWithAnnotation(DiscordController.class).forEach((controllerClassName, controllerObject) -> {
            for(Method method: controllerObject.getClass().getDeclaredMethods()){
                if(!method.isAnnotationPresent(DiscordMapping.class)) continue;
                listener.addObjectMethod(controllerObject, method);
            }
        });

        builder.addEventListeners(listener);

        try {
            builder.build().awaitReady();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
