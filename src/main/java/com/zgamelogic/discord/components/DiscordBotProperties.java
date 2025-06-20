package com.zgamelogic.discord.components;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties class for the discord bot
 *
 * @author Ben Shabowski
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "discord")
public class DiscordBotProperties {
    /**
     * Token for the bot
     */
    private final String token;
    /**
     * Gateway intents for the bot
     *
     * @see net.dv8tion.jda.api.requests.GatewayIntent
     */
    private final String[] gatewayIntents;
    /**
     * Cache flags for the bot
     *
     * @see net.dv8tion.jda.api.utils.cache.CacheFlag
     */
    private final String[] cacheFlags;
    /**
     * Member cache policy for the bot
     *
     * @see net.dv8tion.jda.api.utils.MemberCachePolicy
     */
    private final String memberCachePolicy;
    /**
     * event pass through for the bot
     */
    private final boolean eventPassthrough;

    /**
     * @param token
     * @param gatewayIntents
     * @param cacheFlags
     * @param memberCachePolicy
     * @param eventPassthrough
     */
    public DiscordBotProperties(String token, String[] gatewayIntents, String[] cacheFlags, String memberCachePolicy, boolean eventPassthrough) {
        this.token = token;
        this.gatewayIntents = gatewayIntents;
        this.cacheFlags = cacheFlags;
        this.memberCachePolicy = memberCachePolicy;
        this.eventPassthrough = eventPassthrough;
    }

    public String getToken() {
        return this.token;
    }

    public String[] getGatewayIntents() {
        return this.gatewayIntents;
    }

    public String[] getCacheFlags() {
        return this.cacheFlags;
    }

    public String getMemberCachePolicy() {
        return this.memberCachePolicy;
    }

    public boolean isEventPassthrough() {
        return this.eventPassthrough;
    }
}
