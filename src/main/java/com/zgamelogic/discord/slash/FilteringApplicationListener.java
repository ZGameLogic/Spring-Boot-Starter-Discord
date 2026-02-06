package com.zgamelogic.discord.slash;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationListenerMethodAdapter;

import java.lang.reflect.Method;

class FilteringApplicationListener extends ApplicationListenerMethodAdapter {

    private final CustomEventListener ann;

    FilteringApplicationListener(
        String beanName,
        Class<?> targetClass,
        Method method,
        CustomEventListener ann) {

        super(beanName, targetClass, method);
        this.ann = ann;
    }


    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (!(event instanceof SlashCommandInteractionEventApplicationEvent e)) {
            return;
        }

        if (!matches(e)) {
            return;
        }

        // IMPORTANT: this calls through the Spring proxy
        super.onApplicationEvent(event);
    }

    private boolean matches(SlashCommandInteractionEventApplicationEvent e) {
//        if (!e.getName().equals(ann.id())) return false;
//
//        if (!ann.group().isEmpty()
//            && !ann.group().equals(e.getSubcommandGroup())) return false;
//
//        if (!ann.sub().isEmpty()
//            && !ann.sub().equals(e.getSubcommandName())) return false;

        return true;
    }
}
