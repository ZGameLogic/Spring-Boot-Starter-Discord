package com.zgamelogic.annotations;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@ComponentScan("com.zgamelogic.autoconfigure")
public @interface DiscordController {
}
