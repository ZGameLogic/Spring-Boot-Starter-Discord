package com.zgamelogic.discord.components;

import com.zgamelogic.discord.annotations.*;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.*;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

import static com.zgamelogic.discord.helpers.Translator.isClassValidToObject;

/**
 * DiscordDispatcher is responsible for dispatching events to the appropriate handlers.
 * It uses a handler mapping to find the correct handler for each event and then invokes
 * the handler using the handler adapter.
 */
@Component
public class DiscordDispatcher {
    private static final Logger log = LoggerFactory.getLogger(DiscordDispatcher.class);

    private final ApplicationContext applicationContext;
    private final Map<String, List<ControllerMethod>> mappings;
    private final Map<Class<?>, List<ExceptionMethod>> exceptions;

    public DiscordDispatcher(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        mappings = new HashMap<>();
        exceptions = new HashMap<>();
    }

    @PostConstruct
    private void mapMethods(){
        for (Object bean : applicationContext.getBeansWithAnnotation(DiscordController.class).values()) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                if(!method.isAnnotationPresent(DiscordMapping.class)) continue;
                if(!method.isAnnotationPresent(DiscordMappings.class)) continue;
                // construct a list of annotations on a method
                List<DiscordMapping> annotations = new ArrayList<>();
                DiscordMapping foundAnnotation = AnnotationUtils.findAnnotation(method, DiscordMapping.class);
                DiscordMappings foundAnnotations = AnnotationUtils.findAnnotation(method, DiscordMappings.class);
                if(foundAnnotation != null) annotations.add(foundAnnotation);
                if(foundAnnotations != null) annotations.addAll(Arrays.asList(foundAnnotations.value()));
                for(DiscordMapping mapping : annotations){
                    String key = generateKeyFromMethod(mapping, method);
                    ControllerMethod methodHandle = new ControllerMethod(bean, method);
                    mappings.merge(key, new ArrayList<>(List.of(methodHandle)), (existingList, newList) -> {
                        existingList.addAll(newList);
                        return existingList;
                    });
                }
            }
        }
    }

    @PostConstruct
    private void mapExceptions(){
        for (Object bean : applicationContext.getBeansWithAnnotation(DiscordController.class).values()) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                if(!method.isAnnotationPresent(DiscordExceptionHandler.class)) continue;
                DiscordExceptionHandler annotation = AnnotationUtils.findAnnotation(method, DiscordExceptionHandler.class);
                ExceptionMethod methodHandle = new ExceptionMethod(bean, method, annotation);
                exceptions.merge(bean.getClass(), new ArrayList<>(List.of(methodHandle)), (existingList, newList) -> {
                    existingList.addAll(newList);
                    return existingList;
                });
            }
        }
    }

    public void dispatch(GenericEvent event) {
        String eventKey = generateKeyFromEvent(event);
        mappings.getOrDefault(eventKey, new ArrayList<>()).forEach(controllerMethod -> {
            try {
                Method method = controllerMethod.method();
                Object[] params = resolveParamsForControllerMethod(method, event);
                method.setAccessible(true);
                method.invoke(controllerMethod.controller(), params);
            } catch (Exception e){
                try {
                    throwControllerException(controllerMethod, event, e);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    private void throwControllerException(ControllerMethod controllerMethod, GenericEvent event, Throwable e) throws InvocationTargetException, IllegalAccessException {
        for (ExceptionMethod exceptionMethod : exceptions.getOrDefault(controllerMethod.controller.getClass(), new ArrayList<>())) {
            List<Class<?>> classes = List.of(exceptionMethod.annotation.value());
            Class<?> current = e.getClass();
            while(current != null){
                if(classes.contains(current)){
                    Object[] params = resolveParamsForExceptionMethod(controllerMethod.method, event, e);
                    controllerMethod.method.invoke(controllerMethod.controller, params);
                    return;
                }
                current = current.getSuperclass();
            }
        }
        throw new RuntimeException(e);
    }

    /**
     * Generates a unique key for the method based on the DiscordMapping annotation.
     * @param mapping the DiscordMapping annotation
     * @param method the method to generate the key for
     * @return a unique key for the method
     */
    private String generateKeyFromMethod(DiscordMapping mapping, Method method){
        List<Parameter> JDAParams = Arrays.stream(method.getParameters())
                .filter(parameter -> Event.class.isAssignableFrom(parameter.getType()))
                .toList();
        if(JDAParams.size() != 1){
            log.error("Error when mapping method: {}", method.getName());
            log.error("Discord mappings must have one JDA event parameter.");
            throw new RuntimeException("Discord mappings must have one JDA event parameter");
        }
        Class<?> clazz = JDAParams.get(0).getType();
        return String.format(
            "%s:%s:%s:%s:%s",
            clazz.getSimpleName(),
            mapping.Id(),
            mapping.SubId(),
            mapping.GroupName(),
            mapping.FocusedOption()
        );
    }

    private String generateKeyFromEvent(GenericEvent genericEvent) {
        if (genericEvent instanceof CommandAutoCompleteInteractionEvent event) {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                event.getName(),
                event.getSubcommandName(),
                event.getSubcommandGroup(),
                event.getFocusedOption().getName()
            );
        } else if (genericEvent instanceof GenericCommandInteractionEvent event) {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                event.getName(),
                event.getSubcommandName(),
                event.getSubcommandGroup(),
                ""
            );
        } else if (genericEvent instanceof ModalInteractionEvent event) {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                event.getId(),
                "",
                "",
                ""
            );
        } else if (genericEvent instanceof ButtonInteractionEvent event) {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                event.getId(),
                "",
                "",
                ""
            );
        } else if (genericEvent instanceof StringSelectInteractionEvent event) {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                event.getId(),
                "",
                "",
                event.getInteraction().getSelectedOptions().get(0).getValue()
            );
        } else if (genericEvent instanceof EntitySelectInteractionEvent event) {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                event.getId(),
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

    private Object[] resolveParamsForControllerMethod(Method method, GenericEvent event){
        List<Object> params = new ArrayList<>();
        for(Parameter parameter: method.getParameters()){
            if (Event.class.isAssignableFrom(parameter.getType())) { // if it's the JDA event
                params.add(event);
                continue;
            }
            if(!parameter.isAnnotationPresent(EventProperty.class)) continue; // skip if no annotation
            if(isClassValidToObject(parameter.getType())){ // event property

            } else if(parameter.getType().isRecord()){ // event record

            } else { // event object

            }
        }
        return params.toArray();
    }

    private Object[] resolveParamsForArray(GenericEvent event, Parameter...parameters){
        List<Object> params = new ArrayList<>();
        for(Parameter parameter: parameters){
            if (Event.class.isAssignableFrom(parameter.getType())) { // if it's the JDA event
                params.add(event);
                continue;
            }
            if(!parameter.isAnnotationPresent(EventProperty.class)) continue; // skip if no annotation
            if(isClassValidToObject(parameter.getType())){ // event property

            } else if(parameter.getType().isRecord()){ // event record

            } else { // event object

            }
        }
        return params.toArray();
    }

    private Object[] resolveParamsForExceptionMethod(Method method, GenericEvent event, Throwable throwable){
        // TODO implement
        return new Object[]{};
    }

    private record ControllerMethod(Object controller, Method method){}
    private record ExceptionMethod(Object controller, Method method, DiscordExceptionHandler annotation){}
}
