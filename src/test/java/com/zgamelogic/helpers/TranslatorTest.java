package com.zgamelogic.helpers;

import net.dv8tion.jda.api.requests.GatewayIntent;
import org.junit.jupiter.api.Test;

import static net.dv8tion.jda.api.requests.GatewayIntent.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.zgamelogic.helpers.Translator.*;

class TranslatorTest {

    @Test
    void stringToIntent() {
        assertEquals(GUILD_MEMBERS, Translator.stringToIntent("GUILD_MEMBERS").get());
        assertEquals(GUILD_MODERATION, Translator.stringToIntent("GUILD_MODERATION").get());
        assertEquals(GUILD_EMOJIS_AND_STICKERS, Translator.stringToIntent("GUILD_EMOJIS_AND_STICKERS").get());
        assertEquals(GUILD_WEBHOOKS, Translator.stringToIntent("GUILD_WEBHOOKS").get());
        assertEquals(GUILD_INVITES, Translator.stringToIntent("GUILD_INVITES").get());
        assertEquals(GUILD_VOICE_STATES, Translator.stringToIntent("GUILD_VOICE_STATES").get());
        assertEquals(GUILD_PRESENCES, Translator.stringToIntent("GUILD_PRESENCES").get());
        assertEquals(GUILD_MESSAGES, Translator.stringToIntent("GUILD_MESSAGES").get());
        assertEquals(GUILD_MESSAGE_REACTIONS, Translator.stringToIntent("GUILD_MESSAGE_REACTIONS").get());
        assertEquals(GUILD_MESSAGE_TYPING, Translator.stringToIntent("GUILD_MESSAGE_TYPING").get());
        assertEquals(DIRECT_MESSAGES, Translator.stringToIntent("DIRECT_MESSAGES").get());
        assertEquals(DIRECT_MESSAGE_REACTIONS, Translator.stringToIntent("DIRECT_MESSAGE_REACTIONS").get());
        assertEquals(DIRECT_MESSAGE_TYPING, Translator.stringToIntent("DIRECT_MESSAGE_TYPING").get());
        assertEquals(MESSAGE_CONTENT, Translator.stringToIntent("MESSAGE_CONTENT").get());
        assertEquals(SCHEDULED_EVENTS, Translator.stringToIntent("SCHEDULED_EVENTS").get());
        assertEquals(AUTO_MODERATION_CONFIGURATION, Translator.stringToIntent("AUTO_MODERATION_CONFIGURATION").get());
        assertEquals(AUTO_MODERATION_EXECUTION, Translator.stringToIntent("AUTO_MODERATION_EXECUTION").get());
    }

    @Test
    void stringToCache() {
    }

    @Test
    void stringToMemberCachePolicy() {
    }

    @Test
    void isClassValidToObject() {
    }

    @Test
    void eventOptionToObject() {
    }
}