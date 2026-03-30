package com.zgamelogic.discord.services;

import com.zgamelogic.discord.annotations.mappings.GenericDiscordMapping;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.events.emoji.EmojiAddedEvent;
import net.dv8tion.jda.api.events.emoji.EmojiRemovedEvent;
import net.dv8tion.jda.api.events.emoji.update.EmojiUpdateNameEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class ResourceService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ResourceService.class);

    private final String emojiPath;
    private final ResourcePatternResolver resourcePatternResolver;
    private final Map<String, String> emojiMap;

    public ResourceService(@Value("${discord.resources.emoji:assets/emojis}") String emojiPath, ResourcePatternResolver resourcePatternResolver) {
        this.emojiPath = emojiPath;
        this.resourcePatternResolver = resourcePatternResolver;
        emojiMap = new HashMap<>();
    }

    @GenericDiscordMapping(event = ReadyEvent.class)
    public void ready(ReadyEvent event) throws IOException {
        loadEmojis(event.getJDA());
    }

    @GenericDiscordMapping(event = EmojiAddedEvent.class)
    public void emojiAdd(EmojiAddedEvent event){
        emojiMap.put(event.getEmoji().getName(), event.getEmoji().getAsMention());
    }

    @GenericDiscordMapping(event = EmojiRemovedEvent.class)
    public void emojiRemove(EmojiRemovedEvent event){
        emojiMap.remove(event.getEmoji().getName());
    }

    @GenericDiscordMapping(event = EmojiUpdateNameEvent.class)
    public void emojiUpdate(EmojiUpdateNameEvent event){
        emojiMap.remove(event.getOldName());
        emojiMap.put(event.getNewName(), event.getEmoji().getAsMention());
    }

    private void loadEmojis(JDA bot) throws IOException {
        List<ApplicationEmoji> foundEmojis = new ArrayList<>(bot.retrieveApplicationEmojis().complete());
        Arrays.stream(resourcePatternResolver.getResources("classpath:" + emojiPath + "/*")).forEach(resource -> {
            String filename = resource.getFilename();
            if(filename == null) return;
            String iconName = filename.replace(".png", "");
            Optional<ApplicationEmoji> foundEmoji = foundEmojis.stream().filter(e -> e.getName().equals(iconName)).findFirst();
            foundEmoji.ifPresentOrElse(e -> {
                emojiMap.put(e.getName(), e.getAsMention());
                foundEmojis.remove(e);
            }, () -> {
                log.info("Uploading emoji {} to application", iconName);
                try {
                    Icon icon = Icon.from(resource.getInputStream());
                    CustomEmoji e = bot.createApplicationEmoji(iconName, icon).complete();
                    emojiMap.put(e.getName(), e.getAsMention());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });
        // Remove old emojis
        foundEmojis.forEach(emoji -> {
            log.info("Removing unused emoji {}", emoji.getName());
            emoji.delete().queue();
        });
    }

    public Map<String, String> getEmojiMap(){
        return emojiMap;
    }
}
