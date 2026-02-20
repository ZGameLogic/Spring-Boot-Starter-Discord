package com.zgamelogic.discord.components;

import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
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
    private final GatewayIntent[] gatewayIntents;
    /**
     * Cache flags for the bot
     *
     * @see net.dv8tion.jda.api.utils.cache.CacheFlag
     */
    private final CacheFlag[] cacheFlags;
    /**
     * Member cache policy for the bot
     *
     * @see net.dv8tion.jda.api.utils.MemberCachePolicy
     */
    private final MemberCachePolicy memberCachePolicy;
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
    public DiscordBotProperties(String token, GatewayIntent[] gatewayIntents, CacheFlag[] cacheFlags, MemberCachePolicy memberCachePolicy, boolean eventPassthrough) {
        this.token = token;
        this.gatewayIntents = gatewayIntents;
        this.cacheFlags = cacheFlags;
        this.memberCachePolicy = memberCachePolicy;
        this.eventPassthrough = eventPassthrough;
    }

    public boolean isEventPassthrough() {
        return eventPassthrough;
    }

    public MemberCachePolicy getMemberCachePolicy() {
        return memberCachePolicy;
    }

    public CacheFlag[] getCacheFlags() {
        return cacheFlags;
    }

    public GatewayIntent[] getGatewayIntents() {
        return gatewayIntents;
    }

    public String getToken() {
        return token;
    }
}
