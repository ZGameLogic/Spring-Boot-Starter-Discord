package com.zgamelogic.autoconfigure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@AllArgsConstructor
@ConfigurationProperties(prefix = "discord")
public class DiscordBotProperties {
    private String token;
}
