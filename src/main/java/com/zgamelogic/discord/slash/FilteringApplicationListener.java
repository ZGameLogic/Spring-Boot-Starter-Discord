package com.zgamelogic.discord.slash;

import com.zgamelogic.discord.annotations.DiscordMapping;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationListenerMethodAdapter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

@Slf4j
class FilteringApplicationListener extends ApplicationListenerMethodAdapter {

    private final SlashCommandMapping ann;
    private final Method method;

    FilteringApplicationListener(
        String beanName,
        Class<?> targetClass,
        Method method,
        SlashCommandMapping ann
    ) {
        super(beanName, targetClass, method);
        this.method = method;
        this.ann = ann;
    }

    @Nullable
    @Override
    protected Object[] resolveArguments(ApplicationEvent event) {
//        return super.resolveArguments(event);

        // TODO map method parameters to event method
        return new Object[]{event, "This is a string"};
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (!(event instanceof DiscordEvent e)) return;
        if (!matches(e)) return;
        super.onApplicationEvent(event);
    }

    private boolean matches(DiscordEvent e) {
        String eventKey = generateKeyFromEvent(e.getEvent());
        String methodKey = generateKeyFromMethod(ann, method);
        return eventKey.equals(methodKey);
    }

    // TODO gotta change this to be more flexible with the annotation...
    private String generateKeyFromMethod(Annotation mapping, Method method){
        List<Parameter> JDAParams = Arrays.stream(method.getParameters())
                .filter(parameter -> Event.class.isAssignableFrom(parameter.getType()))
                .toList();
        if(JDAParams.size() != 1 && mapping.Event() == Event.class){
            log.error("Error when mapping method: {}", method.getName());
            log.error("Discord mappings must have one JDA event parameter or include the event class in the annotation.");
            throw new RuntimeException("Discord mappings must have one JDA event parameter or include the event class in the annotation.");
        }
        Class<?> clazz;
        if(!JDAParams.isEmpty()) {
            clazz = JDAParams.get(0).getType();
        } else {
            clazz = mapping.Event();
        }
        String id = "";
        String subId = "";
        String groupName = "";
        String focusedOption = "";

        if(mapping instanceof SlashCommandMapping slashCommandMapping){
            id = slashCommandMapping.id();
            subId = slashCommandMapping.sub();
            groupName = slashCommandMapping.group();
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
