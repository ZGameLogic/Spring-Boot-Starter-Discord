package com.zgamelogic.discord.components;

import com.zgamelogic.discord.annotations.DiscordController;
import com.zgamelogic.discord.annotations.DiscordMapping;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DiscordHandlerMapping implements InitializingBean {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DiscordHandlerMapping.class);

    private final ApplicationContext context;
    private final Map<String, List<MethodHandle>> mapping = new HashMap<>();

    public DiscordHandlerMapping(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void afterPropertiesSet() {
        for (Object bean : context.getBeansWithAnnotation(DiscordController.class).values()) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                DiscordMapping mappingAnn = AnnotationUtils.findAnnotation(method, DiscordMapping.class);
                if (mappingAnn != null) {
                    String key = generateKey(mappingAnn, method);
                    MethodHandle methodHandle = new MethodHandle(bean, method, mappingAnn);
                    mapping.merge(key, List.of(methodHandle), (existingList, newList) -> {
                        existingList.addAll(newList);
                        return existingList;
                    });
                }
            }
        }
    }

    public List<MethodHandle> findHandlerFor(GenericEvent event) {
        String key = generateKeyFromEvent(event);
        return mapping.get(key);
    }

    private String generateKey(DiscordMapping mapping, Method method) {
        List<Parameter> JDAParams = Arrays.stream(method.getParameters())
            .filter(parameter -> Event.class.isAssignableFrom(parameter.getType()))
            .toList();
        if(JDAParams.size() != 1){
            log.error("Error when mapping method: {}", method.getName());
            log.error("Discord mappings must have one JDA event parameter.");
            throw new RuntimeException("Discord mappings must have one JDA event parameter");
        }
        Class<?> clazz = JDAParams.get(0).getType();
        String id = !mapping.Id().isEmpty() ? "-" + mapping.Id() : "";
        String groupId = !mapping.GroupName().isEmpty() ? "-" + mapping.GroupName() : "";
        String subId = !mapping.SubId().isEmpty() ? "-" + mapping.SubId() : "";
        return clazz.getSimpleName() + id + groupId + subId;
    }

    private String generateKeyFromEvent(GenericEvent event){
        // TODO generate key from event
        String name = event.getClass().getSimpleName();

        GenericCommandInteractionEvent j;
        CommandAutoCompleteInteractionEvent e;
        ModalInteractionEvent m;
        ButtonInteractionEvent b;

        StringSelectInteractionEvent si;
        EntitySelectInteractionEvent es;

        return name;
    }

    public record MethodHandle(Object bean, Method method, DiscordMapping mapping) {}
}
