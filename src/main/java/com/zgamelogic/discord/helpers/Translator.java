package com.zgamelogic.discord.helpers;

import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.List;

public abstract class Translator {
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
            Message.Attachment.class
        ).contains(clazz);
    }

    public static Object eventOptionToObject(OptionMapping mapping){
        if(mapping == null) return null;
        return switch(mapping.getType()){
            case STRING -> mapping.getAsString();
            case INTEGER -> mapping.getAsInt();
            case BOOLEAN -> mapping.getAsBoolean();
            case USER -> mapping.getAsUser();
            case CHANNEL -> mapping.getAsChannel();
            case ROLE -> mapping.getAsRole();
            case MENTIONABLE -> mapping.getAsMentionable();
            case ATTACHMENT -> mapping.getAsAttachment();
            default -> null;
        };
    }
}
