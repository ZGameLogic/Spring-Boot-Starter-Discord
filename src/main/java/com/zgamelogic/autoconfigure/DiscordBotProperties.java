package com.zgamelogic.properties;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "discord")
public class DiscordBotProperties {
    private String token;
}
