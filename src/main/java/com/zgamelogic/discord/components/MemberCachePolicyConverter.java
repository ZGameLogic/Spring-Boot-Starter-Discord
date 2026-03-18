package com.zgamelogic.discord.components;

import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class MemberCachePolicyConverter implements Converter<String, MemberCachePolicy> {

    @Override
    public MemberCachePolicy convert(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }

        return switch (source.toUpperCase()) {
            case "NONE" -> MemberCachePolicy.NONE;
            case "ALL" -> MemberCachePolicy.ALL;
            case "OWNER" -> MemberCachePolicy.OWNER;
            case "VOICE" -> MemberCachePolicy.VOICE;
            case "ONLINE" -> MemberCachePolicy.ONLINE;
            case "BOOSTER" -> MemberCachePolicy.BOOSTER;
            case "PENDING" -> MemberCachePolicy.PENDING;
            case "DEFAULT" -> MemberCachePolicy.DEFAULT;
            default -> throw new IllegalArgumentException("Unknown MemberCachePolicy: " + source);
        };
    }
}

