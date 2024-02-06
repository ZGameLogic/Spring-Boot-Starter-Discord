package com.zgamelogic.autoconfigure;

import com.zgamelogic.wrappers.DiscordBotWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DiscordBotProperties.class)
public class DiscordBotAutoConfiguration {

    @Autowired
    public DiscordBotAutoConfiguration(DiscordBotProperties properties){
        System.out.println(properties.getToken());
    }

    @Bean
    public static DiscordBotWrapper discordBotWrapper() {
        System.out.println("Bean");
        return new DiscordBotWrapper("asdf");
    }
}
