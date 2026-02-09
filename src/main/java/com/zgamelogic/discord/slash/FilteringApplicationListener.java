package com.zgamelogic.discord.slash;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationListenerMethodAdapter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Slf4j
class FilteringApplicationListener extends ApplicationListenerMethodAdapter {

    private final ApplicationContext applicationContext;
    private final Annotation ann;
    private final Method method;
    private final String methodKey;

    FilteringApplicationListener(
        String beanName,
        Class<?> targetClass,
        Method method,
        Annotation ann,
        ApplicationContext applicationContext
    ) {
        super(beanName, targetClass, method);
        this.method = method;
        this.ann = ann;
        methodKey = generateKeyFromMethod(ann, method);
        this.applicationContext = applicationContext;
    }

    @Nullable
    @Override
    protected Object[] resolveArguments(ApplicationEvent event) {
        // TODO map method parameters to event method
        return new Object[]{event, "This is a string"};
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (!(event instanceof DiscordEvent e)) return;
        if (!generateKeyFromEvent(e.getEvent()).equals(methodKey)) return;
        super.onApplicationEvent(event);
    }

    // TODO gotta change this to be more flexible with the annotation...
    private String generateKeyFromMethod(Annotation mapping, Method method){
        Class<?> clazz = null;
        String id = "";
        String subId = "";
        String groupName = "";
        String focusedOption = "";

        // TODO include other command mappings for the different annotations
        if(mapping instanceof SlashCommandMapping slashCommandMapping){
            clazz = SlashCommandInteractionEvent.class;
            id = slashCommandMapping.id();
            subId = slashCommandMapping.sub();
            groupName = slashCommandMapping.group();
        } else if (mapping instanceof GenericCommandMapping discordMapping){
            clazz = discordMapping.event() == Event.class ? null : discordMapping.event();
            id = discordMapping.name();
            subId = discordMapping.subName();
            groupName = discordMapping.groupName();
            focusedOption = discordMapping.focussedOption();
        }

        return String.format(
            "%s:%s:%s:%s:%s",
            clazz.getSimpleName(),
            id,
            subId,
            groupName,
            focusedOption
        );
    }

    private String generateKeyFromEvent(GenericEvent genericEvent) {
        if (genericEvent instanceof CommandAutoCompleteInteractionEvent event) {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                event.getName(),
                event.getSubcommandName() != null ? event.getSubcommandName() : "",
                event.getSubcommandGroup() != null ? event.getSubcommandGroup() : "",
                event.getFocusedOption().getName()
            );
        } else if (genericEvent instanceof GenericCommandInteractionEvent event) {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                event.getName(),
                event.getSubcommandName() != null ? event.getSubcommandName() : "",
                event.getSubcommandGroup() != null ? event.getSubcommandGroup() : "",
                ""
            );
        } else if (genericEvent instanceof ModalInteractionEvent event) {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                event.getModalId(),
                "",
                "",
                ""
            );
        } else if (genericEvent instanceof ButtonInteractionEvent event) {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                event.getButton().getCustomId(),
                "",
                "",
                ""
            );
        } else if (genericEvent instanceof GenericSelectMenuInteractionEvent<?, ?> event) {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                event.getSelectMenu().getCustomId(),
                "",
                "",
                ""
            );
        } else {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                "",
                "",
                "",
                ""
            );
        }
    }
}
