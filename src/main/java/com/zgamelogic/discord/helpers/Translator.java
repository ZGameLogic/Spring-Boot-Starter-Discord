package com.zgamelogic.discord.helpers;

import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.List;
import java.util.Optional;

public abstract class Translator {
    public static Optional<GatewayIntent> stringToIntent(String string){
        string = string.toUpperCase().trim();
        return switch (string) {
            case "GUILD_MEMBERS" -> Optional.of(GatewayIntent.GUILD_MEMBERS);
            case "GUILD_MODERATION" -> Optional.of(GatewayIntent.GUILD_MODERATION);
            case "GUILD_EXPRESSIONS" -> Optional.of(GatewayIntent.GUILD_EXPRESSIONS);
            case "GUILD_WEBHOOKS" -> Optional.of(GatewayIntent.GUILD_WEBHOOKS);
            case "GUILD_INVITES" -> Optional.of(GatewayIntent.GUILD_INVITES);
            case "GUILD_VOICE_STATES" -> Optional.of(GatewayIntent.GUILD_VOICE_STATES);
            case "GUILD_PRESENCES" -> Optional.of(GatewayIntent.GUILD_PRESENCES);
            case "GUILD_MESSAGES" -> Optional.of(GatewayIntent.GUILD_MESSAGES);
            case "GUILD_MESSAGE_REACTIONS" -> Optional.of(GatewayIntent.GUILD_MESSAGE_REACTIONS);
            case "GUILD_MESSAGE_TYPING" -> Optional.of(GatewayIntent.GUILD_MESSAGE_TYPING);
            case "DIRECT_MESSAGES" -> Optional.of(GatewayIntent.DIRECT_MESSAGES);
            case "DIRECT_MESSAGE_REACTIONS" -> Optional.of(GatewayIntent.DIRECT_MESSAGE_REACTIONS);
            case "DIRECT_MESSAGE_TYPING" -> Optional.of(GatewayIntent.DIRECT_MESSAGE_TYPING);
            case "MESSAGE_CONTENT" -> Optional.of(GatewayIntent.MESSAGE_CONTENT);
            case "SCHEDULED_EVENTS" -> Optional.of(GatewayIntent.SCHEDULED_EVENTS);
            case "AUTO_MODERATION_CONFIGURATION" -> Optional.of(GatewayIntent.AUTO_MODERATION_CONFIGURATION);
            case "AUTO_MODERATION_EXECUTION" -> Optional.of(GatewayIntent.AUTO_MODERATION_EXECUTION);
            default -> Optional.empty();
        };
    }

    public static Optional<CacheFlag> stringToCache(String string){
        string = string.toUpperCase().trim();
        return switch (string) {
            case "ACTIVITY" -> Optional.of(CacheFlag.ACTIVITY);
            case "VOICE_STATE" -> Optional.of(CacheFlag.VOICE_STATE);
            case "EMOJI" -> Optional.of(CacheFlag.EMOJI);
            case "STICKER" -> Optional.of(CacheFlag.STICKER);
            case "CLIENT_STATUS" -> Optional.of(CacheFlag.CLIENT_STATUS);
            case "MEMBER_OVERRIDES" -> Optional.of(CacheFlag.MEMBER_OVERRIDES);
            case "ROLE_TAGS" -> Optional.of(CacheFlag.ROLE_TAGS);
            case "FORUM_TAGS" -> Optional.of(CacheFlag.FORUM_TAGS);
            case "ONLINE_STATUS" -> Optional.of(CacheFlag.ONLINE_STATUS);
            case "SCHEDULED_EVENTS" -> Optional.of(CacheFlag.SCHEDULED_EVENTS);
            default -> Optional.empty();
        };
    }

    public static Optional<MemberCachePolicy> stringToMemberCachePolicy(String string){
        string = string.toUpperCase().trim();
        return switch(string){
            case "NONE" -> Optional.of(MemberCachePolicy.NONE);
            case "ALL" -> Optional.of(MemberCachePolicy.ALL);
            case "OWNER" -> Optional.of(MemberCachePolicy.OWNER);
            case "ONLINE" -> Optional.of(MemberCachePolicy.ONLINE);
            case "VOICE" -> Optional.of(MemberCachePolicy.VOICE);
            case "BOOSTER" -> Optional.of(MemberCachePolicy.BOOSTER);
            case "PENDING" -> Optional.of(MemberCachePolicy.PENDING);
            case "DEFAULT" -> Optional.of(MemberCachePolicy.DEFAULT);
            default -> Optional.empty();
        };
    }

    public static boolean isClassValidToObject(Class<?> clazz){
        return List.of(
            String.class,
            Integer.class,
            int.class,
            Boolean.class,
            boolean.class,
            User.class,
            Channel.class,
            Role.class,
            IMentionable.class,
            Message.Attachment.class,
            List.class
        ).contains(clazz);
    }

    public static Object eventOptionToObject(OptionMapping mapping){
        if(mapping == null) return null;
        switch(mapping.getType()){
            case STRING -> {
                return mapping.getAsString();
            }
            case INTEGER -> {
                return mapping.getAsInt();
            }
            case BOOLEAN -> {
                return mapping.getAsBoolean();
            }
            case USER -> {
                return mapping.getAsUser();
            }
            case CHANNEL -> {
                return mapping.getAsChannel();
            }
            case ROLE -> {
                return mapping.getAsRole();
            }
            case MENTIONABLE -> {
                return mapping.getAsMentionable();
            }
            case ATTACHMENT -> {
                return mapping.getAsAttachment();
            }
            default -> {
                return null;
            }
        }
    }
}
