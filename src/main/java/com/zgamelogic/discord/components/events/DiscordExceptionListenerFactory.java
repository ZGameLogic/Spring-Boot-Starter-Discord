package com.zgamelogic.discord.components.events;

import com.zgamelogic.discord.annotations.DiscordExceptionHandler;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListenerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DiscordExceptionListenerFactory implements EventListenerFactory, ApplicationContextAware {
    private ApplicationContext applicationContext;
    private final static List<Class<? extends Annotation>> SUPPORTED_ANNOTATIONS = List.of(
        DiscordExceptionHandler.class
    );

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean supportsMethod(@NotNull Method method) {
        return Arrays.stream(method.getAnnotations()).anyMatch(supportedAnnotationsPredicate());
    }

    @NotNull
    @Override
    public ApplicationListener<?> createApplicationListener(@NotNull String beanName, @NotNull Class<?> targetClass, @NotNull Method method) {
        Annotation ann = Arrays.stream(method.getAnnotations()).filter(supportedAnnotationsPredicate()).findFirst().get();
        return new DiscordExceptionApplicationListener(beanName, method, (DiscordExceptionHandler) ann, applicationContext);
    }

    private Predicate<Annotation> supportedAnnotationsPredicate() {
        return ann -> SUPPORTED_ANNOTATIONS.contains(ann.annotationType());
    }
}
