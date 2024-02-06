package com.zgamelogic.configurations;

import com.zgamelogic.properties.DiscordBotProperties;
import com.zgamelogic.wrappers.DiscordBotWrapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(JDA.class)
@ConditionalOnBean(DiscordBotWrapper.class)
@EnableConfigurationProperties(DiscordBotProperties.class)
public class DiscordBotAutoConfiguration {

    @Autowired
    public DiscordBotAutoConfiguration(DiscordBotProperties properties){
        System.out.println("I hope this is getting called?");
        System.out.println(properties.getToken());
    }

//    @Bean
//    public static ListenerAdapter discordEventListener() {
//        return new ListenerAdapter() {};
//    }
}
