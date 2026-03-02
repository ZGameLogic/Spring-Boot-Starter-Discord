package com.zgamelogic.discord.components.events;

import com.zgamelogic.discord.annotations.mappings.*;
import com.zgamelogic.discord.services.ironwood.IronWood;
import com.zgamelogic.discord.services.ironwood.Model;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.utils.data.SerializableData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static com.zgamelogic.discord.helpers.Mapper.resolveParamsForArray;

class DiscordEventApplicationListener implements GenericApplicationListener {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DiscordEventApplicationListener.class);
    private final ApplicationContext applicationContext;
    private final Method method;
    private final DiscordEventKey methodKey;
    private final String beanName;
    private final IronWood ironWood;

    DiscordEventApplicationListener(String beanName, Method method, Annotation ann, ApplicationContext applicationContext) {
        this.beanName = beanName;
        this.method = method;
        this.ironWood = applicationContext.getBean(IronWood.class);
        methodKey = new DiscordEventKey(ann, method);
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(@NotNull ApplicationEvent event) {
        if (!(event instanceof DiscordEvent e)) return;
        if (!methodKey.equals(e.getKey())) return;
        Model model = new Model();
        Object bean = applicationContext.getBean(beanName);
        Object[] params = resolveParamsForControllerMethod(method, e.getEvent(), model);
        try {
            Object returned = method.invoke(bean, params);
            String documentName;
            if(returned instanceof String returnedDocument){
                documentName = returnedDocument;
            } else {
                documentName = Arrays.stream(method.getAnnotations())
                    .map(ann -> {
                        try {
                            Method document = ann.getClass().getDeclaredMethod("document");
                            return (String) document.invoke(ann);
                        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
                            return "";
                        }
                    })
                    .filter(doc -> !doc.isEmpty())
                    .findFirst()
                    .orElse(null);
            }
            if(documentName == null) return;
            SerializableData message = ironWood.generate(documentName, model);
            boolean acknowledged = ((Interaction) e.getEvent()).isAcknowledged();
            /*
            if true, can only send an embed, component, message or poll
            if false, can send embed, component, message, poll or modal
            IReplyCallback has the methods reply, deferReply, editOriginal, deleteOriginal, followupMessage, hook, and sendMessage. Only reply and deferReply can be used if not acknowledged. If acknowledged, can use all methods except reply and deferReply
             */
//            ((IReplyCallback) e.getEvent()).getHook().
//            ((IReplyCallback) e.getEvent())
//            switch(message){
//                case MessageEmbed embed -> {
//                    if(acknowledged){
//                        e.getEvent().getHook().sendMessageEmbeds(embed).queue();
//                    } else {
//                        ((Interaction) e.getEvent()).replyEmbeds(embed).queue();
//                    }
//                }
//            }
            /*
            TODO reply to event, and make sure if its acknowledged or not
             */

        } catch (InvocationTargetException ex) {
            applicationContext.publishEvent(new DiscordExceptionEvent(bean, e.getEvent(), e.getKey(), ex.getTargetException()));
        } catch (IllegalAccessException ex){
            log.error("Unable to call event method", ex);
        } catch (ParserConfigurationException | IOException | NoSuchFieldException | SAXException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        return DiscordEvent.class.isAssignableFrom(eventType.toClass());
    }

    private Object[] resolveParamsForControllerMethod(Method method, GenericEvent event, Model model){
        return resolveParamsForArray(event, null, model, method.getParameters());
    }
}
