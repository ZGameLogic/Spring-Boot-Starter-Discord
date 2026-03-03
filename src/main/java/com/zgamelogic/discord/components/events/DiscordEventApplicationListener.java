package com.zgamelogic.discord.components.events;

import com.zgamelogic.discord.annotations.mappings.*;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
        } catch (InvocationTargetException ex) {
            applicationContext.publishEvent(new DiscordExceptionEvent(bean, e.getEvent(), e.getKey(), ex.getTargetException()));
        } catch (IllegalAccessException ex){
            log.error("Unable to call event method", ex);
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
