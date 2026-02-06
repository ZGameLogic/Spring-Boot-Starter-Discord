package com.zgamelogic.discord.slash;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListenerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
public class CustomEventListenerFactory implements EventListenerFactory {

    @Override
    public boolean supportsMethod(Method method) {
        return AnnotatedElementUtils.hasAnnotation(method, CustomEventListener.class);
    }

    @Override
    public ApplicationListener<?> createApplicationListener(
        String beanName,
        Class<?> targetClass,
        Method method) {

        CustomEventListener ann =
            AnnotatedElementUtils.findMergedAnnotation(method, CustomEventListener.class);

        return new FilteringApplicationListener(beanName, targetClass, method, ann);
    }
}
