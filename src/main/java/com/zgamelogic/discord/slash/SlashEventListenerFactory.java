package com.zgamelogic.discord.slash;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListenerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SlashEventListenerFactory implements EventListenerFactory, ApplicationContextAware {
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean supportsMethod(Method method) {
        return
                AnnotatedElementUtils.hasAnnotation(method, SlashCommandMapping.class) ||
                AnnotatedElementUtils.hasAnnotation(method, GenericCommandMapping.class);
    }

    @Override
    public ApplicationListener<?> createApplicationListener(
        String beanName,
        Class<?> targetClass,
        Method method) {

        // TODO this is pretty bad, need to find a better way to handle multiple annotations
        Annotation ann =
            AnnotatedElementUtils.findMergedAnnotation(method, SlashCommandMapping.class);

        return new FilteringApplicationListener(beanName, targetClass, method, ann, applicationContext);
    }
}
