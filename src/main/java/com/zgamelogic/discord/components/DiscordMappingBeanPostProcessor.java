package com.zgamelogic.discord.components;

import com.zgamelogic.discord.annotations.DiscordMapping;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Scans beans for methods annotated with @DiscordMapping and registers ApplicationListeners
 * that evaluate a SpEL condition built from the annotation and invoke the method when matched.
 */
@Component
public class DiscordMappingBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private ConfigurableApplicationContext context;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final Set<String> registered = new HashSet<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            this.context = (ConfigurableApplicationContext) applicationContext;
        } else {
            throw new IllegalStateException("Requires ConfigurableApplicationContext");
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            DiscordMapping dm = method.getAnnotation(DiscordMapping.class);
            if (dm == null) continue;

            String key = clazz.getName() + "#" + method.getName();
            if (!registered.add(key)) continue; // avoid double registration

            // determine event type
            Class<?> eventType = resolveEventType(dm, method);
            if (eventType == null) continue;

            // build expected string only from non-empty parts (keeps format stable)
            String expected = joinNonEmpty(":", dm.Id(), dm.SubId(), dm.GroupName());

            // build a SpEL condition that concatenates event properties and compares to expected
            // property names in SpEL are lowerCamel matching Java getters: id -> getId()
            String spelCondition = null;
            if (!expected.isEmpty()) {
                // example: "#event.id + ':' + #event.subId + ':' + #event.groupName == 'slash-next:pages:commands'"
                StringBuilder sb = new StringBuilder();
                sb.append("#event.");
                sb.append("id");
                sb.append(" + ':' + #event.");
                sb.append("subId");
                sb.append(" + ':' + #event.");
                sb.append("groupName");
                sb.append(" == '").append(escapeSingleQuote(expected)).append("'");
                spelCondition = sb.toString();
            }

            final String conditionExpr = spelCondition;
            method.setAccessible(true);

            ApplicationListener<Object> listener = event -> {
                if (!eventType.isInstance(event)) return;

                if (conditionExpr != null && !conditionExpr.isEmpty()) {
                    StandardEvaluationContext evalCtx = new StandardEvaluationContext();
                    evalCtx.setVariable("event", event);
                    Expression expr = parser.parseExpression(conditionExpr);
                    Boolean matched;
                    try {
                        matched = expr.getValue(evalCtx, Boolean.class);
                    } catch (Exception ex) {
                        // on evaluation error, skip
                        return;
                    }
                    if (!Boolean.TRUE.equals(matched)) return;
                }

                try {
                    // invoke method; support single-arg methods (the event) or no-arg methods
                    if (method.getParameterCount() == 1) {
                        method.invoke(bean, event);
                    } else {
                        method.invoke(bean);
                    }
                } catch (Exception e) {
                    // swallow or log as appropriate
                    throw new RuntimeException("Failed to invoke @DiscordMapping method", e);
                }
            };

            this.context.addApplicationListener(listener);
        }
        return bean;
    }

    private Class<?> resolveEventType(DiscordMapping dm, Method method) {
        if (dm.Event() != null && !dm.Event().equals(net.dv8tion.jda.api.events.Event.class)) {
            return dm.Event();
        }
        if (method.getParameterCount() >= 1) {
            return method.getParameterTypes()[0];
        }
        return null;
    }

    private static String joinNonEmpty(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String p : parts) {
            if (p == null) p = "";
            if (!p.isEmpty()) {
                if (!first) sb.append(sep);
                sb.append(p);
                first = false;
            } else {
                // include empty placeholders to keep positions (optional)
                if (!first) sb.append(sep);
                sb.append("");
                first = false;
            }
        }
        return sb.toString();
    }

    private static String escapeSingleQuote(String s) {
        return s.replace("'", "''");
    }
