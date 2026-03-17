package com.zgamelogic.discord.components.events;

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

class DiscordEventDispatcher implements GenericApplicationListener {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DiscordEventDispatcher.class);
    private final ApplicationContext applicationContext;
    private final Method method;
    private final DiscordEventKey methodKey;
    private final String beanName;
    private final IronWood ironWood;
    private final Annotation ann;

    DiscordEventDispatcher(String beanName, Method method, Annotation ann, ApplicationContext applicationContext) {
        this.beanName = beanName;
        this.method = method;
        this.ironWood = applicationContext.getBean(IronWood.class);
        methodKey = new DiscordEventKey(ann, method);
        this.applicationContext = applicationContext;
        this.ann = ann;
    }

    @Override
    public void onApplicationEvent(@NotNull ApplicationEvent event) {
        if (!(event instanceof DiscordEvent e)) return;
        Model model = new Model();
        Object[] params = resolveParamsForControllerMethod(method, e.getEvent(), model);
        if (!methodKey.matches(e.getEvent(), method, params)) return;
        Object bean = applicationContext.getBean(beanName);
        try {
            String returned = method.invoke(bean, params) instanceof String document ? document : null;
            ironWood.replyToEvent(returned, ann, method, model, e.getEvent());
        } catch (InvocationTargetException ex) {
            applicationContext.publishEvent(new DiscordExceptionEvent(bean, e.getEvent(), ex.getTargetException()));
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
