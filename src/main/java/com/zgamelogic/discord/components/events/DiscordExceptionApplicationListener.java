package com.zgamelogic.discord.components.events;

import com.zgamelogic.discord.annotations.DiscordExceptionHandler;
import com.zgamelogic.discord.services.ironwood.Model;
import net.dv8tion.jda.api.events.GenericEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static com.zgamelogic.discord.helpers.Mapper.resolveParamsForArray;

class DiscordExceptionApplicationListener implements GenericApplicationListener {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DiscordExceptionApplicationListener.class);
    private final ApplicationContext applicationContext;
    private final Method method;
    private final String beanName;
    private final List<Class<? extends Throwable>> supportedExceptions;

    DiscordExceptionApplicationListener(String beanName, Method method, DiscordExceptionHandler ann, ApplicationContext applicationContext) {
        this.beanName = beanName;
        this.method = method;
        this.applicationContext = applicationContext;
        supportedExceptions = List.of(ann.value());
    }

    @Override
    public void onApplicationEvent(@NotNull ApplicationEvent event) {
        if (!(event instanceof DiscordExceptionEvent e)) return;
        if(e.getSource().getClass() != method.getDeclaringClass()) return;
        if(!supportedExceptions.contains(e.getException().getClass())) return;
        try {
            Model model = new Model();
            Object bean = applicationContext.getBean(beanName);
            Object[] params = resolveParamsForExceptionMethod(method, e.getEvent(), model, e.getException());
            Object returned = method.invoke(bean, params);
            String documentName = null;
            if(returned instanceof String returnedDocument){
                documentName = returnedDocument;
            } else {
                // TODO search the annotations for the document name. Better yet, do that in the constructor and store it as a field
                for(Annotation ann = method.getAnnotations()){
                    Field document = ann.getClass().getDeclaredField("document");
                }
            }
            if(documentName == null) return;
            // TODO create document with model
        } catch (Exception ex) {
            log.error("Unable to call exception method", ex);
        }
    }

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        return DiscordExceptionEvent.class.isAssignableFrom(eventType.toClass());
    }

    private Object[] resolveParamsForExceptionMethod(Method method, GenericEvent event, Model model, Throwable throwable){
        return resolveParamsForArray(event, throwable, model, method.getParameters());
    }
}
