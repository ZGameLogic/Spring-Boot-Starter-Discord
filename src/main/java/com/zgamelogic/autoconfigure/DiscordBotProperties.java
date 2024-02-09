package com.zgamelogic.autoconfigure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties class for the discord bot
 * @author Ben Shabowski
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
@ConfigurationProperties(prefix = "discord")
public class DiscordBotProperties {
    /**
     * Token for the bot
     */
    private String token;
    /**
     * Gateway intents for the bot
     * @see net.dv8tion.jda.api.requests.GatewayIntent
     */
    private String[] gatewayIntents;
    /**
     * Cache flags for the bot
     * @see net.dv8tion.jda.api.utils.cache.CacheFlag
     */
    private String[] cacheFlags;
    /**
     * Member cache policy for the bot
     * @see net.dv8tion.jda.api.utils.MemberCachePolicy
     */
    private String memberCachePolicy;
    /**
     * event pass through for the bot
     */
    private boolean eventPassthrough;
}
