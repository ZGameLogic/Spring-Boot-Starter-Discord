package com.zgamelogic.discord.components;

import com.zgamelogic.discord.annotations.*;
import com.zgamelogic.discord.data.Model;
import com.zgamelogic.discord.services.IronWood;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.*;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.data.SerializableData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

import static com.zgamelogic.discord.helpers.Translator.eventOptionToObject;
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
    private final IronWood ironWood;

    public DiscordDispatcher(ApplicationContext applicationContext, IronWood ironWood) {
        this.applicationContext = applicationContext;
        mappings = new HashMap<>();
        exceptions = new HashMap<>();
        this.ironWood = ironWood;
    }

    @PostConstruct
    private void mapMethods(){
        for (Object bean : applicationContext.getBeansWithAnnotation(DiscordController.class).values()) {
            log.debug("Adding mappings for controller: {}", bean.getClass().getSimpleName());
            for (Method method : bean.getClass().getDeclaredMethods()) {
                if(!method.isAnnotationPresent(DiscordMapping.class) && !method.isAnnotationPresent(DiscordMappings.class)) continue;
                // construct a list of annotations on a method
                List<DiscordMapping> annotations = new ArrayList<>();
                DiscordMapping foundAnnotation = AnnotationUtils.findAnnotation(method, DiscordMapping.class);
                DiscordMappings foundAnnotations = AnnotationUtils.findAnnotation(method, DiscordMappings.class);
                if(foundAnnotation != null) annotations.add(foundAnnotation);
                if(foundAnnotations != null) annotations.addAll(Arrays.asList(foundAnnotations.value()));
                for(DiscordMapping mapping : annotations){
                    String key = generateKeyFromMethod(mapping, method);
                    ControllerMethod methodHandle = new ControllerMethod(bean, method, mapping.Document());
                    log.debug("Adding mappings for method: {}", method.getName());
                    log.debug("\tMapping ID: {}", key);
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
                log.debug("Adding exception mapping with method: {}", method.getName());
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
        log.debug("Mapping ID: {}", eventKey);
        mappings.getOrDefault(eventKey, new ArrayList<>()).forEach(controllerMethod -> {
            Model model = new Model();
            try {
                Method method = controllerMethod.method();
                Object[] params = resolveParamsForControllerMethod(method, event, model);
                method.setAccessible(true);
                Object documentName = method.invoke(controllerMethod.controller(), params);
                if(documentName == null && controllerMethod.document.isEmpty()) return;
                String document = documentName != null ? documentName.toString() : controllerMethod.document;
                SerializableData message = ironWood.generate(document, model);
                if(message instanceof Modal){
                    ((GenericCommandInteractionEvent)event).replyModal(ironWood.generate(document, model)).queue();
                } else if(message instanceof MessageEmbed){
                    ((GenericCommandInteractionEvent)event).replyEmbeds((MessageEmbed) message).addFiles(model.getFileUploads()).queue();
                }
                // TODO component messages
            } catch (InvocationTargetException e){
                try {
                    throwControllerException(controllerMethod, event, e, model);
                } catch (InvocationTargetException | IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void throwControllerException(ControllerMethod controllerMethod, GenericEvent event, InvocationTargetException e, Model model) throws InvocationTargetException, IllegalAccessException {
        for (ExceptionMethod exceptionMethod : exceptions.getOrDefault(controllerMethod.controller.getClass(), new ArrayList<>())) {
            List<Class<?>> classes = List.of(exceptionMethod.annotation.value());
            Class<?> current = e.getTargetException().getClass();
            while(current != null){
                if(classes.contains(current)){
                    Object[] params = resolveParamsForExceptionMethod(exceptionMethod.method, event, model, e.getTargetException());
                    exceptionMethod.method.setAccessible(true);
                    exceptionMethod.method.invoke(controllerMethod.controller, params);
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
                event.getButton().getId(),
                "",
                "",
                ""
            );
        } else if (genericEvent instanceof StringSelectInteractionEvent event) {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                event.getSelectMenu().getId(),
                "",
                "",
                event.getInteraction().getSelectedOptions().get(0).getValue()
            );
        } else if (genericEvent instanceof EntitySelectInteractionEvent event) {
            return String.format(
                "%s:%s:%s:%s:%s",
                genericEvent.getClass().getSimpleName(),
                event.getSelectMenu().getId(),
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

    private Object[] resolveParamsForControllerMethod(Method method, GenericEvent event, Model model){
        return resolveParamsForArray(event, null, model, method.getParameters());
    }

    private Object[] resolveParamsForExceptionMethod(Method method, GenericEvent event, Model model, Throwable throwable){
        return resolveParamsForArray(event, throwable, model, method.getParameters());
    }

    private Object[] resolveParamsForArray(GenericEvent event, Throwable throwable, Model model, Parameter...parameters){
        List<Object> params = new ArrayList<>();
        if (parameters == null) return params.toArray();
        for(Parameter parameter: parameters){
            if (event != null && parameter.getType().isAssignableFrom(event.getClass())) {
                params.add(event);
                continue;
            } else if (Event.class.isAssignableFrom(parameter.getType())){
                params.add(null);
                continue;
            } else if (Model.class.isAssignableFrom(parameter.getType())){
                params.add(model);
                continue;
            }
            if (throwable != null && parameter.getType().isAssignableFrom(throwable.getClass())) { // if it's the throwable
                params.add(throwable);
                continue;
            } else if(Throwable.class.isAssignableFrom(parameter.getType())){
                params.add(null);
                continue;
            }
            EventProperty eventProperty = parameter.getAnnotation(EventProperty.class);
            String name = eventProperty != null && !eventProperty.name().isEmpty() ? eventProperty.name() : parameter.getName();
            if(isClassValidToObject(parameter.getType())){ // event property
                params.add(extractOptionFromEvent(event, name));
            } else { // event record or event class
                Class<?> clazz = parameter.getType();
                if(clazz.getDeclaredConstructors().length == 0) continue;
                Constructor<?> con = clazz.getDeclaredConstructors()[0];
                con.setAccessible(true);
                Object obj;
                try {
                    obj = con.newInstance(resolveParamsForArray(event, throwable, model, con.getParameters()));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
                params.add(obj);
            }
        }
        return params.toArray();
    }

    private Object extractOptionFromEvent(GenericEvent event, String name){
        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            return eventOptionToObject(slashEvent.getOption(name));
        } else if (event instanceof CommandAutoCompleteInteractionEvent autoCompleteEvent) {
            return eventOptionToObject(autoCompleteEvent.getOption(name));
        } else if (event instanceof ModalInteractionEvent modalEvent) {
            if(modalEvent.getValue(name) == null) return null;
            return modalEvent.getValue(name).getAsString();
        }
        return null;
    }

    private record ControllerMethod(Object controller, Method method, String document){}
    private record ExceptionMethod(Object controller, Method method, DiscordExceptionHandler annotation){}
}
