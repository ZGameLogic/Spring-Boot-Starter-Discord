package com.zgamelogic.discord.components.events;

import com.zgamelogic.discord.annotations.DiscordExceptionHandler;
import com.zgamelogic.discord.services.ironwood.IronWood;
import com.zgamelogic.discord.services.ironwood.Model;
import net.dv8tion.jda.api.events.GenericEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import static com.zgamelogic.discord.helpers.Mapper.resolveParamsForArray;

class DiscordExceptionDispatcher implements GenericApplicationListener {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DiscordExceptionDispatcher.class);
    private final ApplicationContext applicationContext;
    private final Method method;
    private final String beanName;
    private final List<Class<? extends Throwable>> supportedExceptions;
    private final DiscordEventKey methodKey;
    private final IronWood ironWood;
    private final Annotation ann;

    DiscordExceptionDispatcher(String beanName, Method method, DiscordExceptionHandler ann, ApplicationContext applicationContext) {
        this.beanName = beanName;
        this.method = method;
        this.applicationContext = applicationContext;
        supportedExceptions = List.of(ann.value());
        this.ironWood = applicationContext.getBean(IronWood.class);
        methodKey = new DiscordEventKey(ann, method);
        this.ann = ann;
    }

    @Override
    public void onApplicationEvent(@NotNull ApplicationEvent event) {
        if (!(event instanceof DiscordExceptionEvent e)) return;
        if(e.getSource().getClass() != method.getDeclaringClass()) return;
        if(!supportedExceptions.contains(e.getException().getClass())) return;
        if(!methodKey.equals(e.getKey())) return;
        try {
            Model model = new Model();
            Object bean = applicationContext.getBean(beanName);
            Object[] params = resolveParamsForExceptionMethod(method, e.getEvent(), model, e.getException());
            String returned = method.invoke(bean, params) instanceof String document ? document : null;
            ironWood.replyToEvent(returned, ann, model, e.getEvent());
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
